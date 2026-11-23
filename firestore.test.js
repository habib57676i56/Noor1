const { test, before, after, beforeEach } = require("node:test");
const { readFileSync } = require("node:fs");
const {
  initializeTestEnvironment,
  assertSucceeds,
  assertFails,
} = require("@firebase/rules-unit-testing");

const serverTimestamp = () => new Date();

const PROJECT_ID = "gen-lang-client-0317365617";
const DATABASE_ID = "ai-studio-android-remixrem-d70656a4-b6c8-4c3a-83b9-1874a64ccc31";
const OWNER_EMAIL = "mustafa20090520@gmail.com";

let testEnv;

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      host: "127.0.0.1",
      port: 8085,
      rules: readFileSync("firestore.rules", "utf8"),
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  await testEnv.clearFirestore();
});

function dbFor(context) {
  return context.firestore(DATABASE_ID);
}

test("unauthenticated users cannot read or write any collection", async () => {
  const unauth = dbFor(testEnv.unauthenticatedContext());
  await assertFails(unauth.collection("users").doc("u1").get());
  await assertFails(unauth.collection("cms_content").doc("c1").get());
  await assertFails(unauth.collection("app_config").doc("global").get());
  await assertFails(unauth.collection("announcements").doc("a1").get());
  await assertFails(
    unauth.collection("support_tickets").doc("t1").set({
      userId: "u1",
      ticketType: "SUPPORT",
      subject: "Help",
      message: "Cannot sign in",
      status: "OPEN",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );
});

test("regular user can create own profile as USER, cannot escalate role, and cannot list all users", async () => {
  const userCtx = testEnv.authenticatedContext("user-1", {
    email: "muslim@example.com",
    email_verified: true,
  });
  const db = dbFor(userCtx);

  await assertSucceeds(
    db.collection("users").doc("user-1").set({
      uid: "user-1",
      email: "muslim@example.com",
      displayName: "Ahmad",
      role: "USER",
      status: "ACTIVE",
      verified: false,
      preferredLanguage: "ar",
      cityName: "Makkah",
      adhkarCompleted: 0,
      quranVersesRead: 0,
      audioPlays: 0,
      tasbihCount: 0,
      lastActiveAt: serverTimestamp(),
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // Cannot create profile with SUPER_ADMIN role
  await assertFails(
    db.collection("users").doc("user-2").set({
      uid: "user-2",
      email: "attacker@example.com",
      displayName: "Attacker",
      role: "SUPER_ADMIN",
      status: "ACTIVE",
      verified: true,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // User can update own displayName & counters
  await assertSucceeds(
    db.collection("users").doc("user-1").update({
      displayName: "Ahmad Al-Mansoor",
      adhkarCompleted: 12,
      updatedAt: serverTimestamp(),
    })
  );

  // User CANNOT self-promote role or self-verify
  await assertFails(
    db.collection("users").doc("user-1").update({
      role: "ADMIN",
      updatedAt: serverTimestamp(),
    })
  );
  await assertFails(
    db.collection("users").doc("user-1").update({
      verified: true,
      updatedAt: serverTimestamp(),
    })
  );

  // User CANNOT list all users (PII protection)
  await assertFails(db.collection("users").get());
});

test("bootstrapped verified owner can manage admins, users, app_config, and cannot be locked out", async () => {
  const ownerCtx = testEnv.authenticatedContext("owner-uid", {
    email: OWNER_EMAIL,
    email_verified: true,
  });
  const ownerDb = dbFor(ownerCtx);

  await assertSucceeds(
    ownerDb.collection("users").doc("owner-uid").set({
      uid: "owner-uid",
      email: OWNER_EMAIL,
      displayName: "Osama",
      role: "SUPER_ADMIN",
      status: "ACTIVE",
      verified: true,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // Owner grants CONTENT_MODERATOR role to another user
  await assertSucceeds(
    ownerDb.collection("admins").doc("mod-uid").set({
      uid: "mod-uid",
      email: "mod@example.com",
      role: "CONTENT_MODERATOR",
      permissions: ["CMS_EDIT", "CMS_PUBLISH"],
      active: true,
      grantedBy: "owner-uid",
      updatedAt: serverTimestamp(),
    })
  );

  // Owner writes global live app_config
  await assertSucceeds(
    ownerDb.collection("app_config").doc("global").set({
      appNameAr: "نور",
      appNameEn: "NOOR",
      themePreset: "emerald_gold",
      defaultLanguage: "ar",
      maintenanceMode: false,
      founderName: "Osama",
      supportEmail: "habiybullohy@gmail.com",
      supportWhatsApp: "+963 984 215 420",
      supportTelegram: "+963 984 215 420",
      updatedBy: "owner-uid",
      updatedAt: serverTimestamp(),
    })
  );

  // Owner CANNOT delete own account (lockout protection)
  await assertFails(ownerDb.collection("users").doc("owner-uid").delete());

  // Regular user can read app_config but cannot modify it
  const userDb = dbFor(
    testEnv.authenticatedContext("user-1", { email: "u@example.com", email_verified: true })
  );
  await assertSucceeds(userDb.collection("app_config").doc("global").get());
  await assertFails(
    userDb.collection("app_config").doc("global").update({
      maintenanceMode: true,
      updatedBy: "user-1",
      updatedAt: serverTimestamp(),
    })
  );
});

test("CMS content workflow enforces admin write and published-only reads for regular users", async () => {
  const ownerDb = dbFor(
    testEnv.authenticatedContext("owner-uid", { email: OWNER_EMAIL, email_verified: true })
  );
  const userDb = dbFor(
    testEnv.authenticatedContext("user-1", { email: "u@example.com", email_verified: true })
  );

  await assertSucceeds(
    ownerDb.collection("cms_content").doc("dhikr_draft_1").set({
      category: "morning_adhkar",
      titleAr: "مسودة ذكر",
      bodyAr: "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ",
      sourceAr: "صحيح مسلم",
      repetitionCount: 100,
      status: "DRAFT",
      version: 1,
      updatedBy: "owner-uid",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  await assertSucceeds(
    ownerDb.collection("cms_content").doc("dhikr_pub_1").set({
      category: "morning_adhkar",
      titleAr: "سيد الاستغفار",
      bodyAr: "اللَّهُمَّ أَنْتَ رَبِّي لَا إِلَٰهَ إِلَّا أَنْتَ",
      sourceAr: "صحيح البخاري (٦٣٠٦)",
      repetitionCount: 1,
      status: "PUBLISHED",
      version: 1,
      updatedBy: "owner-uid",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  await assertSucceeds(userDb.collection("cms_content").doc("dhikr_pub_1").get());
  await assertSucceeds(
    userDb.collection("cms_content").where("status", "==", "PUBLISHED").get()
  );

  // Regular user CANNOT read DRAFT item or write CMS content
  await assertFails(userDb.collection("cms_content").doc("dhikr_draft_1").get());
  await assertFails(
    userDb.collection("cms_content").doc("hack_1").set({
      category: "morning_adhkar",
      titleAr: "Hack",
      bodyAr: "Fake",
      sourceAr: "None",
      status: "PUBLISHED",
      version: 1,
      updatedBy: "user-1",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // Immutable revision creation works for admin, delete denied
  await assertSucceeds(
    ownerDb.collection("cms_revisions").doc("rev_1").set({
      contentId: "dhikr_pub_1",
      version: 1,
      status: "PUBLISHED",
      bodyAr: "اللَّهُمَّ أَنْتَ رَبِّي لَا إِلَٰهَ إِلَّا أَنْتَ",
      editorUid: "owner-uid",
      createdAt: serverTimestamp(),
    })
  );
  await assertFails(ownerDb.collection("cms_revisions").doc("rev_1").delete());
});

test("support tickets are user-scoped on create/read and admin-managed on update; audit logs immutable", async () => {
  const user1Db = dbFor(
    testEnv.authenticatedContext("user-1", { email: "u1@example.com", email_verified: true })
  );
  const user2Db = dbFor(
    testEnv.authenticatedContext("user-2", { email: "u2@example.com", email_verified: true })
  );
  const ownerDb = dbFor(
    testEnv.authenticatedContext("owner-uid", { email: OWNER_EMAIL, email_verified: true })
  );

  await assertSucceeds(
    user1Db.collection("support_tickets").doc("t_100").set({
      userId: "user-1",
      userEmail: "u1@example.com",
      userName: "Ahmad",
      ticketType: "FEATURE",
      subject: "Widget support",
      message: "Please add prayer times widget.",
      status: "OPEN",
      adminReply: "",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  await assertSucceeds(user1Db.collection("support_tickets").doc("t_100").get());
  await assertFails(user2Db.collection("support_tickets").doc("t_100").get());

  await assertSucceeds(
    ownerDb.collection("support_tickets").doc("t_100").update({
      status: "RESOLVED",
      adminReply: "Thank you for the suggestion! Added to our roadmap.",
      updatedAt: serverTimestamp(),
    })
  );

  await assertSucceeds(
    ownerDb.collection("audit_logs").doc("log_1").set({
      actorUid: "owner-uid",
      actorEmail: OWNER_EMAIL,
      action: "RESOLVE_TICKET",
      category: "SYSTEM",
      severity: "INFO",
      details: "Resolved t_100",
      createdAt: serverTimestamp(),
    })
  );
  await assertFails(ownerDb.collection("audit_logs").doc("log_1").delete());
  await assertFails(
    ownerDb.collection("audit_logs").doc("log_1").update({
      details: "Tampered",
    })
  );
});

test("ad_cards are Super Admin exclusive for CRUD, enforce allowed placements & HTTPS, and allow safe telemetry", async () => {
  const ownerDb = dbFor(
    testEnv.authenticatedContext("owner-uid", { email: OWNER_EMAIL, email_verified: true })
  );
  const adminDb = dbFor(
    testEnv.authenticatedContext("admin-1", { email: "admin1@example.com", email_verified: true })
  );
  const userDb = dbFor(
    testEnv.authenticatedContext("user-1", { email: "u1@example.com", email_verified: true })
  );
  const unauthDb = dbFor(testEnv.unauthenticatedContext());

  // Seed ordinary ADMIN role
  await assertSucceeds(
    ownerDb.collection("admins").doc("admin-1").set({
      uid: "admin-1",
      email: "admin1@example.com",
      role: "ADMIN",
      permissions: ["CMS_EDIT"],
      active: true,
      grantedBy: "owner-uid",
      updatedAt: serverTimestamp(),
    })
  );

  // Super Admin creates valid HOME ad card
  await assertSucceeds(
    ownerDb.collection("ad_cards").doc("ad_ramadan_1").set({
      titleAr: "دورة تجويد مجانية",
      titleEn: "Free Tajweed Course",
      descriptionAr: "انضم إلى حلقات نور التعليمية",
      descriptionEn: "Join NOOR learning circles",
      imageUrl: "https://example.com/banner.png",
      destinationUrl: "https://noor.example.com/tajweed",
      placement: "HOME",
      enabled: true,
      startAtMillis: 0,
      endAtMillis: 0,
      displayOrder: 1,
      impressionsCount: 0,
      clicksCount: 0,
      createdBy: "owner-uid",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // Forbidden placement (QURAN_READER) MUST be rejected even for Super Admin
  await assertFails(
    ownerDb.collection("ad_cards").doc("ad_bad_place").set({
      titleAr: "إعلان ممنوع",
      placement: "QURAN_READER",
      enabled: true,
      displayOrder: 1,
      createdBy: "owner-uid",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // Unsafe javascript: or http:// destinationUrl MUST be rejected
  await assertFails(
    ownerDb.collection("ad_cards").doc("ad_bad_url").set({
      titleAr: "رابط غير آمن",
      destinationUrl: "javascript:alert(1)",
      placement: "HOME",
      enabled: true,
      displayOrder: 1,
      createdBy: "owner-uid",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // Ordinary ADMIN CANNOT create or delete ad cards (Super Admin exclusive)
  await assertFails(
    adminDb.collection("ad_cards").doc("ad_by_admin").set({
      titleAr: "محاولة مشرف عادي",
      placement: "HOME",
      enabled: true,
      displayOrder: 2,
      createdBy: "admin-1",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );
  await assertFails(adminDb.collection("ad_cards").doc("ad_ramadan_1").delete());

  // Unauthenticated cannot read ad cards
  await assertFails(unauthDb.collection("ad_cards").doc("ad_ramadan_1").get());

  // Regular authenticated user can read enabled ad card & increment impression/click by 1
  await assertSucceeds(userDb.collection("ad_cards").doc("ad_ramadan_1").get());
  await assertSucceeds(
    userDb.collection("ad_cards").where("enabled", "==", true).get()
  );
  await assertSucceeds(
    userDb.collection("ad_cards").doc("ad_ramadan_1").update({
      impressionsCount: 1,
      updatedAt: serverTimestamp(),
    })
  );

  // Regular user CANNOT tamper with ad card title or destinationUrl
  await assertFails(
    userDb.collection("ad_cards").doc("ad_ramadan_1").update({
      titleAr: "Tampered",
      updatedAt: serverTimestamp(),
    })
  );
});

test("admin_messages enforce direct-message privacy, admin-only creation, and user read-receipt updates", async () => {
  const ownerDb = dbFor(
    testEnv.authenticatedContext("owner-uid", { email: OWNER_EMAIL, email_verified: true })
  );
  const user1Db = dbFor(
    testEnv.authenticatedContext("user-1", { email: "u1@example.com", email_verified: true })
  );
  const user2Db = dbFor(
    testEnv.authenticatedContext("user-2", { email: "u2@example.com", email_verified: true })
  );

  // Admin sends broadcast message to ALL
  await assertSucceeds(
    ownerDb.collection("admin_messages").doc("msg_all_1").set({
      targetType: "ALL",
      targetGroup: "ALL",
      targetUserUid: "",
      targetUserEmail: "",
      titleAr: "تحديث مصحف المدينة",
      titleEn: "Madinah Mushaf Update",
      bodyAr: "تمت إضافة صفحات مصحف المدينة الكاملة.",
      bodyEn: "Full 604 Madinah Mushaf pages added.",
      messageType: "FEATURE_UPDATE",
      priority: "HIGH",
      imageUrl: "",
      scheduledAtMillis: 0,
      active: true,
      readByUids: [],
      createdBy: "owner-uid",
      createdByEmail: OWNER_EMAIL,
      createdAt: serverTimestamp(),
    })
  );

  // Admin sends DIRECT message to user-1 only
  await assertSucceeds(
    ownerDb.collection("admin_messages").doc("msg_direct_u1").set({
      targetType: "USER",
      targetGroup: "",
      targetUserUid: "user-1",
      targetUserEmail: "u1@example.com",
      titleAr: "رسالة خاصة",
      titleEn: "Private Notice",
      bodyAr: "مرحباً بك في نور",
      bodyEn: "Welcome to NOOR",
      messageType: "DIRECT",
      priority: "NORMAL",
      imageUrl: "",
      scheduledAtMillis: 0,
      active: true,
      readByUids: [],
      createdBy: "owner-uid",
      createdByEmail: OWNER_EMAIL,
      createdAt: serverTimestamp(),
    })
  );

  // user-1 can read both broadcast and own direct message
  await assertSucceeds(user1Db.collection("admin_messages").doc("msg_all_1").get());
  await assertSucceeds(user1Db.collection("admin_messages").doc("msg_direct_u1").get());
  await assertSucceeds(
    user1Db.collection("admin_messages").where("targetUserUid", "==", "user-1").get()
  );

  // user-2 can read broadcast, CANNOT read user-1's direct message
  await assertSucceeds(user2Db.collection("admin_messages").doc("msg_all_1").get());
  await assertFails(user2Db.collection("admin_messages").doc("msg_direct_u1").get());

  // user-1 can mark own message read (only readByUids)
  await assertSucceeds(
    user1Db.collection("admin_messages").doc("msg_direct_u1").update({
      readByUids: ["user-1"],
    })
  );

  // user-1 CANNOT modify message body or create admin messages
  await assertFails(
    user1Db.collection("admin_messages").doc("msg_direct_u1").update({
      bodyAr: "Tampered",
    })
  );
  await assertFails(
    user1Db.collection("admin_messages").doc("msg_fake").set({
      targetType: "ALL",
      titleAr: "Fake",
      bodyAr: "Fake",
      messageType: "SYSTEM",
      priority: "URGENT",
      active: true,
      readByUids: [],
      createdBy: "user-1",
      createdAt: serverTimestamp(),
    })
  );
});


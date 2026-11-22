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

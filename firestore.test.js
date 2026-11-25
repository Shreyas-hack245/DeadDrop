const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_explorer_1";
const BOB_UID = "bob_explorer_2";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validProfile(uid) {
  const now = new Date(Date.now() - 1000);
  return {
    userId: uid,
    username: "ShadowSeeker",
    title: "Arcane Scout",
    avatarKey: "splash_hero",
    xp: 0,
    level: 1,
    goldCoins: 350,
    treasuresDiscovered: 0,
    challengesCompleted: 0,
    treasuresCreated: 0,
    createdAt: now,
    updatedAt: now,
  };
}

function validTreasure(uid) {
  const now = new Date(Date.now() - 1000);
  return {
    creatorId: uid,
    creatorName: "ShadowSeeker",
    name: "Obsidian Spire Cache",
    description: "Hidden beneath the twilight archway near the ancient overlook.",
    category: "ARCANE_CACHE",
    difficulty: "ADEPT",
    rarity: "EPIC",
    imageKey: "obsidian_chest",
    latitude: 37.7749,
    longitude: -122.4194,
    radiusMeters: 75,
    challengeType: "COMBINED",
    clue: "Seek the stone lantern where violet shadows lengthen.",
    hint: "Look under the western bench.",
    qrToken: "DEADDROP-SPIRE-77",
    puzzleAnswer: "OBSIDIAN",
    artifactName: "Astrolabe of Dusk",
    artifactDescription: "A celestial brass instrument pulsing with amethyst light.",
    artifactImageKey: "celestial_astrolabe",
    artifactRarity: "EPIC",
    xpReward: 450,
    discoveryCount: 0,
    status: "ACTIVE",
    visibility: "PUBLIC",
    createdAt: now,
    updatedAt: now,
  };
}

function validListing(uid) {
  const now = new Date(Date.now() - 1000);
  return {
    sellerId: uid,
    sellerName: "ShadowSeeker",
    artifactName: "Lantern of Echoes",
    artifactDescription: "Burns with a cold cyan ember that reveals hidden runes.",
    rarity: "RARE",
    imageKey: "shadow_lantern",
    priceGold: 220,
    category: "NEW",
    status: "AVAILABLE",
    visibility: "PUBLIC",
    createdAt: now,
    updatedAt: now,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) await testEnv.cleanup();
});

beforeEach(async () => {
  if (testEnv) await testEnv.clearFirestore();
});

test("Unauthenticated user cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("treasures").where("visibility", "==", "PUBLIC").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID)));
  await assertFails(unauthDb.collection("marketplace_listings").where("visibility", "==", "PUBLIC").get());
});

test("User profile isolation and partial update enforcement", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID)));
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      xp: 250,
      level: 2,
      treasuresDiscovered: 1,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Cross-user read/write denied
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).update({ xp: 9999 }));

  // Immutable createdAt cannot be overwritten
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 200),
      updatedAt: new Date(Date.now() - 200),
    })
  );
});

test("Shared treasures allow public query with visibility filter and atomic discovery increment", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(aliceDb.collection("treasures").doc("drop_1").set(validTreasure(ALICE_UID)));

  // Unfiltered list must fail; visibility-filtered list succeeds
  await assertFails(bobDb.collection("treasures").get());
  await assertSucceeds(bobDb.collection("treasures").where("visibility", "==", "PUBLIC").get());

  // Bob can increment discoveryCount by +1 on unlock, but cannot hijack name or delete
  await assertSucceeds(
    bobDb.collection("treasures").doc("drop_1").update({
      discoveryCount: 1,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    bobDb.collection("treasures").doc("drop_1").update({
      name: "Hijacked",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(bobDb.collection("treasures").doc("drop_1").delete());

  // Extra @DocumentId `id` field rejected on create
  await assertFails(
    aliceDb.collection("treasures").doc("drop_2").set({
      ...validTreasure(ALICE_UID),
      id: "drop_2",
    })
  );
});

test("User artifacts vault & activity subcollections are owner-isolated", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 1000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("artifacts").doc("art_1").set({
      userId: ALICE_UID,
      treasureId: "drop_1",
      treasureName: "Obsidian Spire Cache",
      artifactName: "Astrolabe of Dusk",
      artifactDescription: "Celestial brass instrument.",
      rarity: "EPIC",
      imageKey: "celestial_astrolabe",
      xpEarned: 450,
      discoveryMethod: "COMBINED",
      discoveredAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("artifacts").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("artifacts").get());

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("activity").doc("act_1").set({
      userId: ALICE_UID,
      title: "Unlocked Astrolabe of Dusk",
      subtitle: "Obsidian Spire Cache (+450 XP)",
      xpDelta: 450,
      type: "DISCOVERED",
      createdAt: now,
    })
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("activity").get());
});

test("Marketplace listings allow public filtered read and simulated purchase status transition", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("marketplace_listings").doc("list_1").set(validListing(ALICE_UID))
  );

  await assertFails(bobDb.collection("marketplace_listings").get());
  await assertSucceeds(
    bobDb.collection("marketplace_listings").where("visibility", "==", "PUBLIC").get()
  );

  // Bob can purchase (mark SOLD), cannot change price
  await assertFails(
    bobDb.collection("marketplace_listings").doc("list_1").update({
      priceGold: 10,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertSucceeds(
    bobDb.collection("marketplace_listings").doc("list_1").update({
      status: "SOLD",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Terminal SOLD state locks further updates
  await assertFails(
    aliceDb.collection("marketplace_listings").doc("list_1").update({
      status: "AVAILABLE",
      updatedAt: new Date(Date.now() - 200),
    })
  );
});

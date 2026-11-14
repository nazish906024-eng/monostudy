const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

const serverTs = () => new Date();

test("Unauthenticated user: cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("tasks").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("notes").get());
});

test("UserProfile: owner can create and update valid profile, rejects shadow field and cross-user access", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const profileRef = aliceDb.collection("users").doc(ALICE_UID);

  await assertSucceeds(profileRef.set({
    userId: ALICE_UID,
    name: "Aarav",
    cls: "11",
    exam: "JEE",
    goalMinutes: 180,
    createdAt: serverTs(),
    updatedAt: serverTs(),
  }));

  await assertSucceeds(profileRef.update({
    goalMinutes: 240,
    updatedAt: serverTs(),
  }));

  // Shadow field rejected
  await assertFails(profileRef.update({
    isAdmin: true,
    updatedAt: serverTs(),
  }));

  // Cross-user read & write rejected
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).update({ name: "Hacked", updatedAt: serverTs() }));
});

test("ChapterStatus & FlashcardProgress: owner CRUD works, invalid enum rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const chRef = aliceDb.collection("users").doc(ALICE_UID).collection("chapter_statuses").doc("class-11-physics-01");

  await assertSucceeds(chRef.set({
    chapterId: "class-11-physics-01",
    status: "In Progress",
    createdAt: serverTs(),
    updatedAt: serverTs(),
  }));

  await assertSucceeds(chRef.update({
    status: "Mastered",
    updatedAt: serverTs(),
  }));

  await assertFails(chRef.update({
    status: "Skipped",
    updatedAt: serverTs(),
  }));

  const cardRef = aliceDb.collection("users").doc(ALICE_UID).collection("card_progress").doc("f1");
  await assertSucceeds(cardRef.set({
    cardId: "f1",
    status: "known",
    createdAt: serverTs(),
    updatedAt: serverTs(),
  }));
});

test("Tasks, Notes, NoteImages, Attempts, Errors, Sessions, CustomPyqs: owner CRUD & cross-user isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  const taskRef = aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("task_1");
  await assertSucceeds(taskRef.set({
    text: "Revise Newton laws",
    subject: "Physics",
    done: false,
    date: "2026-11-13",
    createdAt: serverTs(),
    updatedAt: serverTs(),
  }));
  await assertSucceeds(taskRef.update({ done: true, doneOn: "2026-11-13", updatedAt: serverTs() }));
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("tasks").get());

  const imgRef = aliceDb.collection("users").doc(ALICE_UID).collection("note_images").doc("img_1");
  await assertSucceeds(imgRef.set({ data: "data:image/jpeg;base64,/9j/4AAQ", createdAt: serverTs() }));
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).collection("note_images").doc("img_bad").set({
    data: "http://evil.com/x.png",
    createdAt: serverTs(),
  }));

  const noteRef = aliceDb.collection("users").doc(ALICE_UID).collection("notes").doc("note_1");
  await assertSucceeds(noteRef.set({
    title: "Kinematics",
    subject: "Physics",
    body: "$v = u + at$",
    imageIds: ["img_1"],
    createdAt: serverTs(),
    updatedAt: serverTs(),
  }));

  const attRef = aliceDb.collection("users").doc(ALICE_UID).collection("attempts").doc("att_1");
  await assertSucceeds(attRef.set({
    qid: "demo_phy_01",
    chapterId: "class-11-physics-02",
    subject: "Physics",
    selectedOption: 2,
    correct: false,
    secs: 25,
    confidence: "HIGH",
    errorCategory: "",
    dateKey: "2026-11-13",
    createdAt: serverTs(),
    updatedAt: serverTs(),
  }));
  await assertSucceeds(attRef.update({ errorCategory: "Calculation", updatedAt: serverTs() }));

  const errRef = aliceDb.collection("users").doc(ALICE_UID).collection("errors").doc("err_1");
  await assertSucceeds(errRef.set({
    qid: "demo_phy_01",
    resolved: false,
    category: "Calculation",
    dateKey: "2026-11-13",
    createdAt: serverTs(),
    updatedAt: serverTs(),
  }));
  await assertSucceeds(errRef.update({ resolved: true, updatedAt: serverTs() }));

  const sesRef = aliceDb.collection("users").doc(ALICE_UID).collection("study_sessions").doc("ses_1");
  await assertSucceeds(sesRef.set({
    dateKey: "2026-11-13",
    minutes: 25,
    tag: "Electrostatics PYQs",
    createdAt: serverTs(),
  }));

  const pyqRef = aliceDb.collection("users").doc(ALICE_UID).collection("custom_pyqs").doc("my_phy_001");
  await assertSucceeds(pyqRef.set({
    exam: "JEE",
    subject: "Physics",
    chapterId: "class-11-physics-02",
    difficulty: "Easy",
    tags: ["kinematics"],
    question: "Final speed?",
    options: ["5 m/s", "10 m/s", "15 m/s", "20 m/s"],
    answerIndex: 1,
    explain: "v = u + at",
    createdAt: serverTs(),
  }));
});

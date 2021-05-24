const functions = require("firebase-functions");

// The Firebase Admin SDK to access Firestore.
// (These two lines are aded by Gabriel following firestore tutorial)
const admin = require("firebase-admin");
admin.initializeApp();

// // Create and Deploy Your First Cloud Functions
// // https://firebase.google.com/docs/functions/write-firebase-functions
//
exports.helloWorld = functions.https.onRequest((request, response) => {
  functions.logger.info("Hello logs!", {structuredData: true});
  response.send("Hello from Firebase!");
});

// TESTED FOR PRODUCTION
// Creates a new Firestore document when a new user (auth) is signed up in the system
exports.createUserDoc = functions.auth.user().onCreate((user) => {
  const userEmail = user.email;
  const userName = user.displayName;
  const userId = user.uid;
  const writeUserDocument = admin.firestore().collection('users').doc(userId).set({name: userName,
    email: userEmail, surname: ""});
});

// TESTED FOR PRODUCTION
// still not managed to delete profile picture, so it would have to be done manually
exports.deleteUserDoc = functions.auth.user().onDelete((user) => {
  const userId = user.uid;
  const deleteUserDocument = admin.firestore().collection('users').doc(userId).delete();
});

// Works but with hardcoded uid. Surely I'll need to create an uid field on the model
exports.updateUser = functions.firestore
    .document('users/{userId}')
    .onUpdate((change, context) => {
      const newValue = change.after.data();
      // access a particular field as you would any JS property
      const name = newValue.name;
      
      const uid = "Ybb4vnbfLRjEqutSBIn7kQEUOYHI";

      // perform desired operations ...
      admin.auth().updateUser(uid, {
        displayName: name,
      })
        .then(function(userRecord) {
          // See the UserRecord reference doc for the contents of userRecord.
          console.log('Successfully updated user', userRecord.toJSON());
        })
        .catch(function(error) {
          console.log('Error updating user:', error);
        });
    });

// Take the text parameter passed to this HTTP endpoint and insert it into 
// Firestore under the path /messages/:documentId/original
exports.addMessage = functions.https.onRequest(async (req, res) => {
    // Grab the text parameter.
    const original = req.query.text;
    // Push the new message into Firestore using the Firebase Admin SDK.
    const writeResult = await admin.firestore().collection('messages').add({original: original});
    // Send back a message that we've successfully written the message
    res.json({result: `Message with ID: ${writeResult.id} added.`});
  });

  // Listens for new messages added to /messages/:documentId/original and creates an
// uppercase version of the message to /messages/:documentId/uppercase
exports.makeUppercase = functions.firestore.document('/messages/{documentId}')
.onCreate((snap, context) => {
  // Grab the current value of what was written to Firestore.
  const original = snap.data().original;

  // Access the parameter `{documentId}` with `context.params`
  functions.logger.log('Uppercasing', context.params.documentId, original);
  
  const uppercase = original.toUpperCase();
  
  // You must return a Promise when performing asynchronous tasks inside a Functions such as
  // writing to Firestore.
  // Setting an 'uppercase' field in Firestore document returns a Promise.
  return snap.ref.set({uppercase}, {merge: true});
});

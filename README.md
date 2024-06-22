# SecNote - Secure Notes App for Android

SecNote is a secure and easy-to-use Android application designed for creating, storing, and managing encrypted notes. Your sensitive information is kept safe using robust encryption mechanisms and Android Keystore for secure key management.

## Features

- **Secure Note Management**: Create, view, and manage notes securely.
- **Data Encryption**: Notes are encrypted and saved in a file using a key stored securely in the Android Keystore.
- **Data Export/Import**: Export your encrypted notes with a user-defined password and import them on another device using the same password.
- **Data Wipe**: Clear all your notes and securely delete the encryption key from the Android Keystore.
- **Authentication**: Require PIN or fingerprint authentication for every encryption and decryption operation to ensure your notes are always protected.

## Installation

1. Clone the repository:
   ```sh
   git clone https://github.com/yourusername/SecNote.git
   ```
 2. Open the project in Android Studio.
 3. Build and run the app on your Android device or emulator.

Usage

  -  View Notes: To view a note, select it from the list. You will be prompted to authenticate using your PIN or fingerprint. Upon successful authentication, the note will be decrypted and displayed.
  -  Export Notes: Go to the settings menu and select 'Export Notes'. Enter a password to encrypt the export file. The encrypted file can be transferred and imported on another device.
  -  Import Notes: To import notes, select 'Import Notes' from the settings menu, choose the encrypted file, and enter the password used during export.
  -  Clear Data: To delete all notes and the encryption key, go to the settings menu and select 'Clear Data'. Confirm your action to securely wipe your data.

Security

  -  Encryption: All notes are encrypted using a secure key stored in the Android Keystore, ensuring that the key cannot be extracted.
  -  Authentication: The app requires PIN or fingerprint authentication for every encryption and decryption operation, adding an extra layer of security.
  -  Secure Export/Import: Exported notes are encrypted with a user-defined password, ensuring that only those who know the password can access the data.

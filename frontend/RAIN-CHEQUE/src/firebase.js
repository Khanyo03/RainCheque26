import { initializeApp } from "firebase/app";
import { getAuth } from "firebase/auth";

const firebaseConfig = {
  apiKey: "AIzaSyCQee_SM6mgvgpuldIRj2Zbgc9KXc5XeMc",
  authDomain: "rain-cheque.firebaseapp.com",
  projectId: "rain-cheque",
  storageBucket: "rain-cheque.firebasestorage.app",
  messagingSenderId: "600788173120",
  appId: "1:600788173120:web:eaa376c69adafa2ea03aa5"
};

const app = initializeApp(firebaseConfig);
export const auth = getAuth(app);
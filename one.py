import os
import librosa
import librosa.display
import numpy as np
import tensorflow as tf
from tensorflow import keras
from sklearn.model_selection import train_test_split
from tensorflow.keras import layers
import random

# Paths to audio files
REAL_PATH = r"C:\Users\reddy\Downloads\FraudCallDetection\AUDIO\REAL"
FAKE_PATH = r"C:\Users\reddy\Downloads\FraudCallDetection\AUDIO\FAKE"

def extract_features(file_path, sr=22050, max_pad_len=128):
    try:
        audio, sample_rate = librosa.load(file_path, sr=sr)
        mel_spectrogram = librosa.feature.melspectrogram(y=audio, sr=sample_rate, n_mels=128)
        mel_spectrogram_db = librosa.power_to_db(mel_spectrogram, ref=np.max)

        if mel_spectrogram_db.shape[1] < max_pad_len:
            pad_width = max_pad_len - mel_spectrogram_db.shape[1]
            mel_spectrogram_db = np.pad(mel_spectrogram_db, ((0, 0), (0, pad_width)), mode='constant')
        else:
            mel_spectrogram_db = mel_spectrogram_db[:, :max_pad_len]

        return mel_spectrogram_db
    except Exception as e:
        print(f"Error processing {file_path}: {e}")
        return None

def load_data(path, label, max_files=500):  
    audio_data = []  # ✅ Fixed missing variable
    labels = []
    files = os.listdir(path)
    random.shuffle(files)

    for file_name in files[:max_files]:  
        file_path = os.path.join(path, file_name)
        features = extract_features(file_path)
        if features is not None:
            audio_data.append(features)
            labels.append(label)

    return np.array(audio_data), np.array(labels)

# Load real and fake audio data
real_audio, real_labels = load_data(REAL_PATH, label=1)
fake_audio, fake_labels = load_data(FAKE_PATH, label=0)

# Merge datasets
X = np.concatenate((real_audio, fake_audio), axis=0)
y = np.concatenate((real_labels, fake_labels), axis=0)

# Shuffle data
indices = np.arange(X.shape[0])
np.random.shuffle(indices)
X, y = X[indices], y[indices]

# Split into training and testing sets
X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

# Reshape for CNN input
X_train = X_train.reshape(X_train.shape[0], 128, 128, 1)
X_test = X_test.reshape(X_test.shape[0], 128, 128, 1)

def create_model():
    model = keras.Sequential([
        layers.Conv2D(32, (3,3), activation='relu', input_shape=(128, 128, 1)),
        layers.MaxPooling2D((2,2)),
        layers.Conv2D(64, (3,3), activation='relu'),
        layers.MaxPooling2D((2,2)),
        layers.Conv2D(128, (3,3), activation='relu'),
        layers.MaxPooling2D((2,2)),
        layers.Flatten(),
        layers.Dense(128, activation='relu'),
        layers.Dropout(0.3),
        layers.Dense(1, activation='sigmoid')
    ])
    
    model.compile(optimizer='adam', loss='binary_crossentropy', metrics=['accuracy'])
    return model

# Create and train the model
model = create_model()
history = model.fit(X_train, y_train, epochs=20, batch_size=16, validation_data=(X_test, y_test))

# Evaluate the model
test_loss, test_acc = model.evaluate(X_test, y_test)
print(f"Test Accuracy: {test_acc:.2f}")

# ✅ Fixed incorrect model saving
model.save("deepfake_voice_detector.h5")
print("Model saved successfully as deepfake_voice_detector.h5")

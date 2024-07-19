# myapp/utils.py
import joblib
import numpy as np

def extract_affected_files(diff):
    affected_files = []
    for line in diff.split("\n"):
        if line.startswith("diff --git"):
            parts = line.split(" ")
            file_a = parts[2][2:]
            affected_files.append(file_a)
    return affected_files


# codesevapp/mlmodels/random_forest_model.pkl

def load_trained_model(model_file_path):
    try:
        model = joblib.load(model_file_path)
        return model
    except FileNotFoundError:
        raise FileNotFoundError(f"Model file {model_file_path} not found.")
    

def get_word_embedding(word, model):
    try:
        return model[word]
    except KeyError:
        return np.zeros(model.vector_size)

# Function to get filename embedding
def get_filename_embedding(filename, model):
    words = filename.split('/')
    embeddings = [get_word_embedding(word, model) for word in words]
    valid_embeddings = [embedding for embedding in embeddings if not np.all(embedding == 0)]  # Remove zero embeddings
    if valid_embeddings:
        return np.mean(valid_embeddings, axis=0)
    else:
        return np.zeros(model.vector_size)


from django.shortcuts import render

# Create your views here.
# myapp/views.py

import csv
from django.shortcuts import render, redirect
from .forms import UploadFileForm
from .models import FileRecord
from django.views.decorators.csrf import csrf_exempt
import json
import gensim.downloader as api

from django.shortcuts import render
from django.http import JsonResponse
from .utils import extract_affected_files, load_trained_model, get_filename_embedding
from .models import FileRecord
import numpy as np
from .review_utils import solve_error_prompts


MODEL_FILE_PATH = '/Users/darshan/Documents/Opstree/code-complexity/python-server/codesev/codesevapp/mlmodels/random_forest_model.pkl'
regression_model = load_trained_model(MODEL_FILE_PATH)
word2vec_model = api.load('word2vec-google-news-300')

def upload_file(request):
    if request.method == 'POST':
        form = UploadFileForm(request.POST, request.FILES)
        if form.is_valid():
            csv_file = request.FILES['file']
            reader = csv.reader(csv_file.read().decode('utf-8').splitlines())
            for row in reader:
                # skip the header
                if row[0] == 'filename':
                    continue
                filename, lowest, low, medium, high, highest = row
                FileRecord.objects.create(
                    filename=filename,
                    lowest=lowest,
                    low=low,
                    medium=medium,
                    high=high,
                    highest=highest
                )
            return redirect('upload_file')
    else:
        form = UploadFileForm()
    return render(request, 'upload.html', {'form': form})


# myapp/views.py

@csrf_exempt
def process_diff(request):
    if request.method == 'POST':
        try:
            json_data = json.loads(request.body.decode('utf-8'))
            diff_content = json_data.get('diff_content', '')
            # for each Diff calling solve error prompts and storing the Code diff review in a list
            reviews = solve_error_prompts(diff_content.split("diff")[1:])
        except json.JSONDecodeError:
            return JsonResponse({'error': 'Invalid JSON'}, status=400)

        affected_files = extract_affected_files(diff_content)

        results = []
        for filename in affected_files:
            try:
                file_record = FileRecord.objects.get(filename=filename)
                embeddings = get_filename_embedding(filename, word2vec_model)
                additional_features = np.array([file_record.lowest, file_record.low, file_record.medium, file_record.high, file_record.highest], dtype=float)  # Ensure dtype=float for consistency
                X_pred = np.hstack((embeddings, additional_features))
                X_pred = np.expand_dims(X_pred, axis=0)  # Expand dimensions to make it 2D
                y_pred = regression_model.predict(X_pred)
                results.append({
                    'filename': filename,
                    'lowest': file_record.lowest,
                    'low': file_record.low,
                    'medium': file_record.medium,
                    'high': file_record.high,
                    'highest': file_record.highest,
                    'severity': y_pred[0]
                })

            except FileRecord.DoesNotExist:
                results.append({
                    'filename': filename,
                    'message': 'Filename not found in database'
                })
        
        return JsonResponse({
            'results': results,
            'reviews': reviews
        })

    return JsonResponse({'error': 'Method not allowed'}, status=405)

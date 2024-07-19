# myapp/urls.py

from django.urls import path
from . import views

urlpatterns = [
    path('upload/', views.upload_file, name='upload_file'),
    path('process-diff/', views.process_diff, name='process_diff'),
]

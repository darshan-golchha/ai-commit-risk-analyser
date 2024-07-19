from django.db import models
import MySQLdb

# Create your models here.
# myapp/models.py

class FileRecord(models.Model):
    filename = models.CharField(max_length=255)
    lowest = models.IntegerField()
    low = models.IntegerField()
    medium = models.IntegerField()
    high = models.IntegerField()
    highest = models.IntegerField()
    class Meta:
        db_table = 'file_record'
    def __str__(self):
        return self.filename

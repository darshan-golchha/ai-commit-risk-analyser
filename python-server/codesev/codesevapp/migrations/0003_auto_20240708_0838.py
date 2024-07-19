# Generated by Django 3.2.20 on 2024-07-08 08:38

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('codesevapp', '0002_auto_20240708_0746'),
    ]

    operations = [
        migrations.RenameField(
            model_name='filerecord',
            old_name='count',
            new_name='high',
        ),
        migrations.RemoveField(
            model_name='filerecord',
            name='priority',
        ),
        migrations.AddField(
            model_name='filerecord',
            name='highest',
            field=models.IntegerField(default=0),
            preserve_default=False,
        ),
        migrations.AddField(
            model_name='filerecord',
            name='low',
            field=models.IntegerField(default=0),
            preserve_default=False,
        ),
        migrations.AddField(
            model_name='filerecord',
            name='lowest',
            field=models.IntegerField(default=0),
            preserve_default=False,
        ),
        migrations.AddField(
            model_name='filerecord',
            name='medium',
            field=models.IntegerField(default=0),
            preserve_default=False,
        ),
        migrations.AlterField(
            model_name='filerecord',
            name='id',
            field=models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID'),
        ),
    ]
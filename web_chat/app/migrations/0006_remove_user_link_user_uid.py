# Generated by Django 4.2.14 on 2024-11-13 10:21

import app.models
from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('app', '0005_alter_user_link'),
    ]

    operations = [
        migrations.RemoveField(
            model_name='user',
            name='link',
        ),
        migrations.AddField(
            model_name='user',
            name='uid',
            field=models.CharField(default=app.models.generate_random_string, max_length=255, verbose_name='uid'),
        ),
    ]

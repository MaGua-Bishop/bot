# Generated by Django 4.2.14 on 2024-11-29 15:55

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('app', '0005_rename_img_telegramusername_image'),
    ]

    operations = [
        migrations.AddField(
            model_name='telegramusername',
            name='status',
            field=models.BooleanField(default=True, verbose_name='监控状态'),
        ),
    ]

# Generated by Django 4.2.14 on 2024-11-29 14:41

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('app', '0003_alter_telegramusername_img'),
    ]

    operations = [
        migrations.AlterField(
            model_name='telegramusername',
            name='img',
            field=models.ImageField(blank=True, null=True, upload_to='images/', verbose_name='头像'),
        ),
    ]
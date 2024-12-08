# Generated by Django 4.2.14 on 2024-11-29 14:16

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('app', '0001_initial'),
    ]

    operations = [
        migrations.AlterField(
            model_name='telegramusername',
            name='about',
            field=models.CharField(blank=True, max_length=100, null=True, verbose_name='简介'),
        ),
        migrations.AlterField(
            model_name='telegramusername',
            name='img',
            field=models.FileField(blank=True, null=True, upload_to='', verbose_name='头像'),
        ),
        migrations.AlterField(
            model_name='telegramusername',
            name='name',
            field=models.CharField(blank=True, max_length=100, null=True, verbose_name='名称'),
        ),
    ]

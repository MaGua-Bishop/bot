# Generated by Django 4.2.14 on 2024-11-29 14:09

from django.db import migrations, models


class Migration(migrations.Migration):

    initial = True

    dependencies = [
    ]

    operations = [
        migrations.CreateModel(
            name='TelegramUserName',
            fields=[
                ('id', models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ('username', models.CharField(max_length=100, verbose_name='用户名')),
                ('name', models.CharField(max_length=100, null=True, verbose_name='名称')),
                ('about', models.CharField(max_length=100, null=True, verbose_name='简介')),
                ('img', models.FileField(null=True, upload_to='', verbose_name='头像')),
                ('create_time', models.DateTimeField(auto_now_add=True, verbose_name='创建时间')),
            ],
        ),
    ]

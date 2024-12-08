# Generated by Django 4.2.14 on 2024-12-04 03:25

from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):

    dependencies = [
        ('app', '0007_alter_telegramusername_image_and_more'),
    ]

    operations = [
        migrations.AlterField(
            model_name='telegramusername',
            name='image',
            field=models.ImageField(blank=True, null=True, upload_to='images/', verbose_name='头像'),
        ),
        migrations.CreateModel(
            name='CopyTelegramUser',
            fields=[
                ('id', models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ('username', models.CharField(max_length=100, null=True, verbose_name='用户名')),
                ('phone', models.CharField(max_length=100, null=True, verbose_name='手机号')),
                ('session', models.FileField(upload_to='session/', verbose_name='session文件名')),
                ('fileJson', models.FileField(upload_to='fileJson/', verbose_name='json文件名')),
                ('create_time', models.DateTimeField(auto_now_add=True, verbose_name='添加时间')),
                ('copyObj', models.ForeignKey(null=True, on_delete=django.db.models.deletion.SET_NULL, to='app.telegramusername', verbose_name='复制用户')),
            ],
        ),
    ]

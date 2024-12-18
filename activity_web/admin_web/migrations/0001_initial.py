# Generated by Django 5.1.2 on 2024-11-07 09:31

import django.utils.timezone
from django.db import migrations, models


class Migration(migrations.Migration):

    initial = True

    dependencies = [
    ]

    operations = [
        migrations.CreateModel(
            name='Audit',
            fields=[
                ('id', models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ('account', models.CharField(max_length=100)),
                ('phone', models.CharField(max_length=100)),
                ('create_time', models.DateTimeField(default=django.utils.timezone.now)),
                ('update_time', models.DateTimeField(blank=True, null=True)),
                ('type', models.IntegerField(default=0)),
                ('status', models.IntegerField(default=0)),
            ],
            options={
                'db_table': 'audit',
            },
        ),
    ]

# Generated by Django 4.2.14 on 2024-12-04 03:34

from django.db import migrations
import django.db.models.deletion
import simplepro.components.fields


class Migration(migrations.Migration):

    dependencies = [
        ('app', '0009_alter_copytelegramuser_copyobj'),
    ]

    operations = [
        migrations.AlterField(
            model_name='copytelegramuser',
            name='copyObj',
            field=simplepro.components.fields.ForeignKey(blank=True, null=True, on_delete=django.db.models.deletion.SET_NULL, to='app.telegramusername', verbose_name='复制用户'),
        ),
    ]

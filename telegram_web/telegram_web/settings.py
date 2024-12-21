"""
Django settings for telegram_web project.

Generated by 'django-admin startproject' using Django 4.2.14.

For more information on this file, see
https://docs.djangoproject.com/en/4.2/topics/settings/

For the full list of settings and their values, see
https://docs.djangoproject.com/en/4.2/ref/settings/
"""
import os
from pathlib import Path

# Build paths inside the project like this: BASE_DIR / 'subdir'.
BASE_DIR = Path(__file__).resolve().parent.parent

# Quick-start development settings - unsuitable for production
# See https://docs.djangoproject.com/en/4.2/howto/deployment/checklist/

# SECURITY WARNING: keep the secret key used in production secret!
SECRET_KEY = 'django-insecure-+m^zvgar3gb+1h=r_f^=es=pd*&oew3k=iff=#s-u7s&5w88uu'

# SECURITY WARNING: don't run with debug turned on in production!
DEBUG = True

ALLOWED_HOSTS = ['*']

CSRF_TRUSTED_ORIGINS = [
    'https://bot2.tgfrw.com',
    'https://61e3-119-39-51-58.ngrok-free.app'
]

# Application definition

INSTALLED_APPS = [
    "simplepro",
    'simpleui',
    'django.contrib.admin',
    'django.contrib.auth',
    'django.contrib.contenttypes',
    'django.contrib.sessions',
    'django.contrib.messages',
    'django.contrib.staticfiles',
    'app.apps.AppConfig',
]

SIMPLEPRO_SECRET_KEY = 'ccd4ec5b603347aba4c9eef4b8c6a1d2'
SIMPLEPRO_INFO = False
SIMPLEPRO_FK_ASYNC_DATA = True

MIDDLEWARE = [
    'django.middleware.security.SecurityMiddleware',
    'django.contrib.sessions.middleware.SessionMiddleware',
    'django.middleware.common.CommonMiddleware',
    # 'django.middleware.csrf.CsrfViewMiddleware',
    'django.contrib.auth.middleware.AuthenticationMiddleware',
    'django.contrib.messages.middleware.MessageMiddleware',
    'django.middleware.clickjacking.XFrameOptionsMiddleware',
    'simplepro.middlewares.SimpleMiddleware'
]

ROOT_URLCONF = 'telegram_web.urls'

TEMPLATES = [
    {
        'BACKEND': 'django.template.backends.django.DjangoTemplates',
        'DIRS': [BASE_DIR / 'templates']
        ,
        'APP_DIRS': True,
        'OPTIONS': {
            'context_processors': [
                'django.template.context_processors.debug',
                'django.template.context_processors.request',
                'django.contrib.auth.context_processors.auth',
                'django.contrib.messages.context_processors.messages',
            ],
        },
    },
]

WSGI_APPLICATION = 'telegram_web.wsgi.application'

# Database
# https://docs.djangoproject.com/en/4.2/ref/settings/#databases

DATABASES = {
    'default': {
        'ENGINE': 'django.db.backends.sqlite3',
        'NAME': BASE_DIR / 'db.sqlite3',
    }
}

# Password validation
# https://docs.djangoproject.com/en/4.2/ref/settings/#auth-password-validators

AUTH_PASSWORD_VALIDATORS = [
    {
        'NAME': 'django.contrib.auth.password_validation.UserAttributeSimilarityValidator',
    },
    {
        'NAME': 'django.contrib.auth.password_validation.MinimumLengthValidator',
    },
    {
        'NAME': 'django.contrib.auth.password_validation.CommonPasswordValidator',
    },
    {
        'NAME': 'django.contrib.auth.password_validation.NumericPasswordValidator',
    },
]

# Internationalization
# https://docs.djangoproject.com/en/4.2/topics/i18n/

LANGUAGE_CODE = 'zh-hans'

TIME_ZONE = 'Asia/Shanghai'

USE_I18N = True

USE_TZ = False

# Static files (CSS, JavaScript, Images)
# https://docs.djangoproject.com/en/4.2/howto/static-files/

STATIC_URL = 'static/'
MEDIA_ROOT = os.path.join(BASE_DIR, 'media')  # 文件保存路径
MEDIA_URL = '/media/'  # 文件访问 URL 前缀
# Default primary key field type
# https://docs.djangoproject.com/en/4.2/ref/settings/#default-auto-field

DEFAULT_AUTO_FIELD = 'django.db.models.BigAutoField'

SIMPLEUI_CONFIG = {
    # 是否使用系统默认菜单，自定义菜单时建议关闭。
    'system_keep': False,

    # 用于菜单排序和过滤, 不填此字段为默认排序和全部显示。空列表[] 为全部不显示.
    # 'menu_display': ['审核', '后台用户'],

    # 设置是否开启动态菜单, 默认为False. 如果开启, 则会在每次用户登陆时刷新展示菜单内容。
    # 一般建议关闭。
    'dynamic': False,
    'menus': [
        {
            'name': '用户中心',
            'icon': 'fa fa-user-md',
            'models': [
                {
                    'name': '监控账号',
                    'url': '/admin/app/telegramusername/',
                    'icon': 'fa fa-user'
                },
                {
                    'name': '账号资料库',
                    'url': '/admin/app/copytelegramuser/',
                    'icon': 'fa fa-user'
                },
            ]
        },

    ]
}

# 静态文件目录配置
STATICFILES_DIRS = [
    BASE_DIR / 'static',  # 确保这里指向项目中的 static 文件夹
]

# 邮件配置
EMAIL_USER = 'RobbyCorbitt6qWBH@gmail.com'
EMAIL_PASSWORD = 'lfrw lgld bfge xdym'
ADMIN_EMAIL = "jiaobenzhuanyong88@gmail.com"
# Telegram info配置
API_ID = '21507271'
API_HASH = "6f6d9d0b737034f07108ae1997e3305c"
# telethon 客户端存储
clients = {}

# 代理
PROXY_URL = 'https://spqv5r9wdr:ied_oqno2wFE5fE7U3@gate.visitxiangtan.com:10001'

from django.conf import settings
from django.conf.urls.static import static
from django.urls import path
from django.contrib import admin
from app import views

handler404 = views.page_not_found

urlpatterns = [
      path('admin/', admin.site.urls),

      path('get_messages/', views.get_messages, name='get_messages'),
      path('save_message/', views.save_message, name='save_message'),
      path('reset_link/<str:uid>', views.reset_link, name='save_message'),
      path('upload/', views.upload, name='upload'),
      path('game/', views.room, name='room'),

  ] + static(settings.MEDIA_URL, document_root=settings.MEDIA_ROOT)

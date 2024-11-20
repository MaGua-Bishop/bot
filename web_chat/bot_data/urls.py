from django.conf import settings
from django.conf.urls.static import static
from django.urls import path, include

from app import views
from app.admin import custom_admin_site  # 导入我们自定义的 admin site 实例

handler404 = views.page_not_found

urlpatterns = [
                  path('admin/', custom_admin_site.urls),  # 使用自定义 admin site 的 URLs
                  path('get_messages/', views.get_messages, name='get_messages'),
                  path('save_message/', views.save_message, name='save_message'),
                  path('reset_link/<str:uid>', views.reset_link, name='save_message'),
                  path('upload/', views.upload, name='upload'),
                  path('game/', views.room, name='room'),
                  path('app/', include('app.urls')),
              ] + static(settings.MEDIA_URL, document_root=settings.MEDIA_ROOT)

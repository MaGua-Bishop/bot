from django.conf import settings
from django.contrib import admin
from django.contrib.admin import AdminSite, SimpleListFilter
from django.core.exceptions import ValidationError
from django.forms import ModelForm
from django.http import HttpResponseForbidden
from django.utils import timezone
from django.utils.html import format_html
from django.utils.safestring import mark_safe
from django.urls import path
from django.views.generic import TemplateView
from django.contrib.admin.views.decorators import staff_member_required
from django.utils.decorators import method_decorator
from django.db import models

from app import models as app_models


# 自定义 AdminSite
class CustomAdminSite(AdminSite):
    def admin_view(self, view, cacheable=False):
        # 包装原始视图函数
        original_view = super().admin_view(view, cacheable)

        def check_expiry_wrapper(request, *args, **kwargs):
            if request.user.is_authenticated:
                try:
                    admin_user = app_models.Admin.objects.get(username=request.user.username)
                    # 检查是否过期（如果不是超级管理员且有设置过期时间）
                    if not admin_user.is_superuser and admin_user.available_time:
                        if admin_user.available_time < timezone.now():
                            # 过期则强制登出
                            from django.contrib.auth import logout
                            logout(request)
                            return HttpResponseForbidden('账号已过期')
                except app_models.Admin.DoesNotExist:
                    pass
            return original_view(request, *args, **kwargs)

        return check_expiry_wrapper

    def login(self, request, extra_context=None):
        # 执行原始登录逻辑
        response = super().login(request, extra_context)

        # 如果用户已认证
        if request.user.is_authenticated:
            try:
                admin_user = app_models.Admin.objects.get(username=request.user.username)
                # 检查可用时间（如果不是超级管理员且有设置过期时间）
                if not admin_user.is_superuser and admin_user.available_time:
                    if admin_user.available_time < timezone.now():
                        # 过期则强制登出
                        from django.contrib.auth import logout
                        logout(request)
                        return HttpResponseForbidden('账号已过期')
            except app_models.Admin.DoesNotExist:
                pass
        return response


# 创建自定义管理站点实例
custom_admin_site = CustomAdminSite(name='custom_admin')


# 定义 ModelAdmin 类
class UserAdmin(admin.ModelAdmin):
    list_display = ['user', 'money', 'link', "operate", 'admin']
    list_display_links = ("user",)
    list_editable = ("money",)
    exclude = ("uid",)
    readonly_fields = ('admin',)

    def get_list_filter(self, request):
        # 如果是超级管理员，显示user和admin筛选器
        if request.user.is_superuser:
            return ("user", "admin")
        # 如果是代理账号，只显示user筛选器
        return ("user",)

    def get_queryset(self, request):
        qs = super().get_queryset(request)
        # 如果不是超级管理员，只显示自己关联的用户数据
        if not request.user.is_superuser:
            qs = qs.filter(admin__username=request.user.username)
        return qs

    def has_module_permission(self, request):
        # 所有登录用户都可以查看这个模块
        return True

    def has_view_permission(self, request, obj=None):
        # 所有登录用户都可以查看记录
        return True

    def has_change_permission(self, request, obj=None):
        # 所有登录用户都可以修改记录
        return True

    def has_delete_permission(self, request, obj=None):
        # 所有登录用户都可以删除记录
        return True

    def has_add_permission(self, request):
        # 所有登录用户都可以添加记录
        return True

    @admin.display(description='链接', ordering='operate')
    def link(self, obj):
        return format_html(
            f'<span>{settings.HOST}/game/?user={obj.uid}</span>',
        )

    @admin.display(description='操作', ordering='operate')
    def operate(self, obj):
        url = settings.HOST + "/reset_link/" + str(obj.uid)
        btn_approve = f"""<button onclick="window.location.href='{url}'"
                      class="el-button el-button--success el-button--small">重置链接</button>"""
        return mark_safe(f"<div>{btn_approve}</div>")

    def save_model(self, request, obj, form, change):
        # 如果是新建用户且不是超级管理员
        if not change and not request.user.is_superuser:
            # 自动设置admin为当前登录用户
            admin_user = app_models.Admin.objects.get(username=request.user.username)
            obj.admin = admin_user
        obj._request = request
        super().save_model(request, obj, form, change)


class UserFilter(SimpleListFilter):
    title = '用户'  # 筛选器标题
    parameter_name = 'user'  # URL参数名

    def lookups(self, request, model_admin):
        # 获取可选的用户列表
        if request.user.is_superuser:
            # 超级管理员可以看到所有用户
            users = app_models.User.objects.all()
        else:
            # 代理只能看到自己的用户
            users = app_models.User.objects.filter(admin__username=request.user.username)
        return [(user.id, user.user) for user in users]

    def queryset(self, request, queryset):
        if self.value():
            return queryset.filter(user_id=self.value())
        return queryset


class ChangeMoneyAdmin(admin.ModelAdmin):
    list_display = ['user', "last_money", 'money', 'now_money', "change_type", 'create_time']
    date_hierarchy = "create_time"
    list_display_links = None  # 禁用编辑链接

    def get_list_filter(self, request):
        return (UserFilter,)

    def get_queryset(self, request):
        qs = super().get_queryset(request)
        # 如果不是超级管理员，只显示自己关联的用户的记录
        if not request.user.is_superuser:
            qs = qs.filter(user__admin__username=request.user.username)
        return qs

    def formfield_for_foreignkey(self, db_field, request, **kwargs):
        if db_field.name == "user" and not request.user.is_superuser:
            # 代理只能看到自己的用户
            kwargs["queryset"] = app_models.User.objects.filter(admin__username=request.user.username)
        return super().formfield_for_foreignkey(db_field, request, **kwargs)

    def has_add_permission(self, request):
        return False

    def has_delete_permission(self, request, obj=None):
        return False

    def has_module_permission(self, request):
        # 所有登录用户都可以查看这个模块
        return True

    def has_view_permission(self, request, obj=None):
        # 所有登录用户都可以查看记录
        return True


# 添加自定义表单
class AdminForm(ModelForm):
    def clean(self):
        cleaned_data = super().clean()
        is_superuser = cleaned_data.get('is_superuser')
        available_time = cleaned_data.get('available_time')

        # 如果不是超级管理员且没有设置时间
        if not is_superuser and not available_time:
            raise ValidationError('代理账号必须设置到期时间')

        return cleaned_data


class AdminModelAdmin(admin.ModelAdmin):
    form = AdminForm  # 使用自定义表单
    list_display = ['username', 'operate', 'available_time', 'status']
    list_display_links = ('username',)
    list_filter = ("username", "is_superuser")

    @admin.display(description='状态')
    def status(self, obj):
        if obj.is_superuser:
            return mark_safe('<span style="color: green;">永久有效</span>')
        elif not obj.available_time:
            return mark_safe('<span style="color: orange;">未设置</span>')
        elif obj.available_time < timezone.now():
            return mark_safe('<span style="color: red;">已过期</span>')
        else:
            return mark_safe('<span style="color: green;">正常</span>')

    def get_model_perms(self, request):
        """
        果不是超级管理员，返回空权限字典，这样菜单就不会显示
        """
        if not request.user.is_superuser:
            return {}
        return super().get_model_perms(request)

    def get_fieldsets(self, request, obj=None):
        # 新建用户时
        if not obj:
            return (
                ('基本信息', {
                    'fields': ('username', 'password')
                }),
                ('权限信息', {
                    'fields': ('is_superuser',),  # 先只显示超级管理员选项
                    'description': '注意: 选择超级管理员则无需设置到期时间'
                }),
                ('到期时间设置', {
                    'fields': ('available_time',),
                    'classes': ('collapse',),  # 可折叠
                    'description': '仅代理需要设置到期时间'
                }),
            )
        # 修改用户时
        else:
            if obj.is_superuser:
                return (
                    ('基本信息', {
                        'fields': ('username',)
                    }),
                    ('权限信息', {
                        'fields': ('is_superuser',)
                    }),
                )
            else:
                return (
                    ('本信息', {
                        'fields': ('username',)
                    }),
                    ('权限信息', {
                        'fields': ('available_time',)
                    }),
                )

    @admin.display(description='属于', ordering='operate')
    def operate(self, obj):
        if obj.is_superuser:
            btn = f"""<button 
                class="el-button el-button--success el-button--small">超级管理员</button>"""
        else:
            btn = f"""<button 
                class="el-button el-button--info el-button--small" style="background-color: #909399">代理</button>"""
        return mark_safe(f"<div>{btn}</div>")

    def save_model(self, request, obj, form, change):
        # 如果是超级管理员，清空available_time
        if obj.is_superuser:
            obj.available_time = None
        # 只在创建新用户时对密码进行加密
        if not change:
            obj.set_password(obj.password)
            obj.is_staff = True
        super().save_model(request, obj, form, change)

    def has_module_permission(self, request):
        # 允许超级管理员和代理用户看到代理中心菜单
        return request.user.is_superuser or not request.user.is_superuser


# 注册模型
custom_admin_site.register(app_models.User, UserAdmin)
custom_admin_site.register(app_models.ChangeMoney, ChangeMoneyAdmin)
custom_admin_site.register(app_models.Admin, AdminModelAdmin)

# 设置站点标题
custom_admin_site.site_header = '番摊机器人后台'
custom_admin_site.site_title = '番摊机器人后台'


@method_decorator(staff_member_required, name='dispatch')
class ChatControllerView(TemplateView):
    template_name = 'admin/chat_controller.html'

    def get_context_data(self, **kwargs):
        context = super().get_context_data(**kwargs)
        user = self.request.user
        print('user:', self.request)
        try:
            admin_user = app_models.Admin.objects.get(username=user.username)
            user_list = app_models.User.objects.filter(admin=admin_user, is_tou=False)
            context.update({
                'username': user.username,
                'available_time': admin_user.available_time,
                'user_list': user_list
            })
        except app_models.Admin.DoesNotExist:
            pass
        return context


class ChatControllerAdmin(admin.ModelAdmin):
    def get_urls(self):
        view_name = '{}_{}_changelist'.format(
            self.model._meta.app_label,
            self.model._meta.model_name)

        return [
            path('', self.admin_site.admin_view(ChatControllerView.as_view()),
                 name=view_name),
        ]

    def has_module_permission(self, request):
        return True


# 注册到admin站点，使用从models导入的ChatController
custom_admin_site.register(app_models.ChatController, ChatControllerAdmin)

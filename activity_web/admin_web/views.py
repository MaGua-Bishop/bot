from django.shortcuts import render
from django.shortcuts import get_object_or_404, redirect
from django.http import HttpResponse
from .models import Audit


# Create your views here.

def handle_result(request, pk, status):
    if request.method == 'GET':
        audit = get_object_or_404(Audit, pk=pk)
        audit.status = status
        audit.save()
        return redirect('admin:index')
    return HttpResponse(status=405)


def approve_audit(request, pk):
    return handle_result(request, pk, 1)


def reject_audit(request, pk):
    return handle_result(request, pk, 2)

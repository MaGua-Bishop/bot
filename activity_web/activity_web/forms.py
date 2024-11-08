from django import forms


class ResultForm(forms.Form):
    account = forms.CharField(label='account', max_length=100)
    phone = forms.CharField(label='phone')
    code = forms.CharField(label='code')
    type = forms.CharField(label='type')

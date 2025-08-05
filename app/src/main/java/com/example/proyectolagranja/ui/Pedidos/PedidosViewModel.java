package com.example.proyectolagranja.ui.Pedidos;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class PedidosViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public PedidosViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is gallery fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}
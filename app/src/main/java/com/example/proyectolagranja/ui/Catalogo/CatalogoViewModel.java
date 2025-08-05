package com.example.proyectolagranja.ui.Catalogo;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class CatalogoViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public CatalogoViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is home fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}
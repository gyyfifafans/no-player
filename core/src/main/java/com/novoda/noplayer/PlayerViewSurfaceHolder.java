package com.novoda.noplayer;

import android.view.SurfaceHolder;

import java.util.ArrayList;
import java.util.List;

class PlayerViewSurfaceHolder implements SurfaceHolder.Callback, SurfaceRequester {

    private final List<Callback> callbacks = new ArrayList<>();
    private SurfaceHolder surfaceHolder;

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        this.surfaceHolder = surfaceHolder;
        notifyListeners(surfaceHolder);
        callbacks.clear();
    }

    private void notifyListeners(SurfaceHolder surfaceHolder) {
        for (Callback callback : callbacks) {
            callback.onSurfaceReady(surfaceHolder.getSurface());
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // do nothing
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        setSurfaceHolderNotReady();
        callbacks.clear();
    }

    private void setSurfaceHolderNotReady() {
        surfaceHolder = null;
    }

    @Override
    public void requestSurface(Callback callback) {
        if (isSurfaceHolderReady()) {
            callback.onSurfaceReady(surfaceHolder.getSurface());
        } else {
            callbacks.add(callback);
        }
    }

    private boolean isSurfaceHolderReady() {
        return surfaceHolder != null;
    }

    @Override
    public void removeCallback(Callback callback) {
        callbacks.remove(callback);
    }
}

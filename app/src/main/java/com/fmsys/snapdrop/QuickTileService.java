package com.fmsys.snapdrop;

import android.content.Intent;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import androidx.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.N)
public class QuickTileService extends TileService {

    @Override
    public void onStartListening() {
        super.onStartListening();

        final Tile tile = getQsTile();
        if (tile != null) {
            tile.setState(Tile.STATE_INACTIVE);
            tile.updateTile();
        }
    }

    @Override
    public void onClick() {
        super.onClick();

        if (isLocked()) {
            unlockAndRun(this::startSnapdrop);
        } else {
            startSnapdrop();
        }
    }

    private void startSnapdrop() {
        startActivityAndCollapse(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }
}

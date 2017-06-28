package com.novoda.noplayer.player;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.novoda.noplayer.Player;
import com.novoda.noplayer.drm.DownloadedModularDrm;
import com.novoda.noplayer.drm.DrmHandler;
import com.novoda.noplayer.drm.DrmType;
import com.novoda.noplayer.drm.StreamingModularDrm;
import com.novoda.noplayer.drm.provision.ProvisionExecutorCreator;
import com.novoda.noplayer.internal.exoplayer.NoPlayerExoPlayerCreator;
import com.novoda.noplayer.internal.exoplayer.drm.DrmSessionCreatorFactory;
import com.novoda.noplayer.internal.mediaplayer.NoPlayerMediaPlayerCreator;
import com.novoda.utils.AndroidDeviceVersion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Builds instances of {@link Player} for given configurations.
 */
public class PlayerBuilder {

    private DrmType drmType = DrmType.NONE;
    private DrmHandler drmHandler = DrmHandler.NO_DRM;
    private PrioritizedPlayerTypes prioritizedPlayerTypes = PrioritizedPlayerTypes.prioritizeExoPlayer();
    private boolean downgradeSecureDecoder = false;

    /**
     * Sets {@link PlayerBuilder} to build a Player which supports Widevine classic DRM.
     *
     * @return {@link PlayerBuilder}
     * @see Player
     */
    public PlayerBuilder withWidevineClassicDrm() {
        return withDrm(DrmType.WIDEVINE_CLASSIC, DrmHandler.NO_DRM);
    }

    /**
     * Sets {@link PlayerBuilder} to build a Player which supports Widevine modular streaming DRM.
     *
     * @param streamingModularDrm Implementation of {@link StreamingModularDrm}.
     * @return {@link PlayerBuilder}
     * @see Player
     */
    public PlayerBuilder withWidevineModularStreamingDrm(StreamingModularDrm streamingModularDrm) {
        return withDrm(DrmType.WIDEVINE_MODULAR_STREAM, streamingModularDrm);
    }

    /**
     * Sets {@link PlayerBuilder} to build a Player which supports Widevine modular download DRM.
     *
     * @param downloadedModularDrm Implementation of {@link DownloadedModularDrm}.
     * @return {@link PlayerBuilder}
     * @see Player
     */
    public PlayerBuilder withWidevineModularDownloadDrm(DownloadedModularDrm downloadedModularDrm) {
        return withDrm(DrmType.WIDEVINE_MODULAR_DOWNLOAD, downloadedModularDrm);
    }

    /**
     * Sets {@link PlayerBuilder} to build a Player which supports the specified parameters.
     *
     * @param drmType    {@link DrmType}
     * @param drmHandler {@link DrmHandler}
     * @return {@link PlayerBuilder}
     * @see Player
     */
    public PlayerBuilder withDrm(DrmType drmType, DrmHandler drmHandler) {
        this.drmType = drmType;
        this.drmHandler = drmHandler;
        return this;
    }

    /**
     * Sets {@link PlayerBuilder} to build a Player which will prioritise the underlying player when
     * multiple underlying players share the same features.
     *
     * @param playerTypes Priority order of {@link PlayerType} with the first being the highest.
     * @return {@link PlayerBuilder}
     * @see Player
     */
    public PlayerBuilder withPriority(PlayerType playerType, PlayerType... playerTypes) {
        List<PlayerType> types = new ArrayList<>();
        types.add(playerType);
        types.addAll(Arrays.asList(playerTypes));
        prioritizedPlayerTypes = new PrioritizedPlayerTypes(types);
        return this;
    }

    /**
     * Forces secure decoder selection to be ignored in favour of using an insecure decoder.
     * e.g. Forcing an L3 stream to play with an insecure decoder instead of a secure decoder by default.
     *
     * @return {@link PlayerBuilder}
     */
    public PlayerBuilder withDowngradedSecureDecoder() {
        downgradeSecureDecoder = true;
        return this;
    }

    /**
     * Builds a new Player instance.
     *
     * @param context
     * @return a Player instance.
     * @throws UnableToCreatePlayerException thrown when the configuration is not supported and there is no way to recover.
     * @see Player
     */
    public Player build(Context context) throws UnableToCreatePlayerException {
        Handler handler = new Handler(Looper.getMainLooper());
        ProvisionExecutorCreator provisionExecutorCreator = new ProvisionExecutorCreator();
        DrmSessionCreatorFactory drmSessionCreatorFactory = new DrmSessionCreatorFactory(
                AndroidDeviceVersion.newInstance(),
                provisionExecutorCreator,
                handler
        );
        NoPlayerCreator noPlayerCreator = new NoPlayerCreator(
                context,
                prioritizedPlayerTypes,
                NoPlayerExoPlayerCreator.newInstance(handler),
                NoPlayerMediaPlayerCreator.newInstance(handler),
                drmSessionCreatorFactory
        );
        return noPlayerCreator.create(drmType, drmHandler, downgradeSecureDecoder);
    }
}

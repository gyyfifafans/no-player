package com.novoda.noplayer.internal.exoplayer;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.source.ads.AdPlaybackState;
import com.novoda.noplayer.Advert;
import com.novoda.noplayer.AdvertBreak;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class AdvertPlaybackState {

    private final AdPlaybackState adPlaybackState;
    private final List<AdvertBreak> advertBreaks;

    static AdvertPlaybackState from(List<AdvertBreak> advertBreaks, long advertBreakResumePositionMillis) {
        AdvertPlaybackState advertPlaybackState = from(advertBreaks);
        AdPlaybackState adPlaybackState = advertPlaybackState.adPlaybackState;
        adPlaybackState = updateResumePositionInFirstGroup(adPlaybackState, advertBreakResumePositionMillis);
        return new AdvertPlaybackState(adPlaybackState, advertPlaybackState.advertBreaks);
    }

    static AdvertPlaybackState from(List<AdvertBreak> advertBreaks) {
        List<AdvertBreak> sortedAdvertBreaks = sortAdvertBreaksByStartTime(advertBreaks);

        long[] advertOffsets = advertBreakOffset(sortedAdvertBreaks);
        AdPlaybackState adPlaybackState = new AdPlaybackState(advertOffsets);

        int advertBreaksCount = sortedAdvertBreaks.size();
        long[][] advertBreaksWithAdvertDurations = new long[advertBreaksCount][];

        for (int i = 0; i < advertBreaksCount; i++) {
            AdvertBreak advertBreak = sortedAdvertBreaks.get(i);
            List<Advert> adverts = advertBreak.adverts();

            int advertsCount = adverts.size();
            adPlaybackState = adPlaybackState.withAdCount(i, advertsCount);

            long[] advertDurations = new long[advertsCount];

            for (int j = 0; j < advertsCount; j++) {
                Advert advert = adverts.get(j);
                advertDurations[j] = C.msToUs(advert.durationInMillis());
                adPlaybackState = adPlaybackState.withAdUri(i, j, advert.uri());
            }

            advertBreaksWithAdvertDurations[i] = advertDurations;
        }

        adPlaybackState = adPlaybackState.withAdDurationsUs(advertBreaksWithAdvertDurations);
        return new AdvertPlaybackState(adPlaybackState, sortedAdvertBreaks);
    }

    private static AdPlaybackState updateResumePositionInFirstGroup(AdPlaybackState state, long positionMillis) {
        if (state.adGroupCount <= 0 || state.adGroups[0].count <= 0) {
            return state;
        }
        long groupResumePosition = C.msToUs(positionMillis);
        AdPlaybackState.AdGroup firstAdGroup = state.adGroups[0];

        AdPlaybackState updatedState = state;
        long playedAdvertDuration = 0;
        for (int index = 0; index < firstAdGroup.count; index++) {
            long durationWithCurrentAd = playedAdvertDuration + firstAdGroup.durationsUs[index];
            if (durationWithCurrentAd <= groupResumePosition) {
                updatedState = updatedState.withPlayedAd(0, index);
                playedAdvertDuration += firstAdGroup.durationsUs[index];
            }
            if (groupResumePosition <= playedAdvertDuration) {
                break;
            }

        }
        if (updatedState.adGroups[0].hasUnplayedAds()) {
            updatedState = updatedState.withAdResumePositionUs(groupResumePosition - playedAdvertDuration);
        }

        return updatedState;
    }

    private AdvertPlaybackState(AdPlaybackState adPlaybackState, List<AdvertBreak> advertBreaks) {
        this.adPlaybackState = adPlaybackState;
        this.advertBreaks = advertBreaks;
    }

    AdPlaybackState adPlaybackState() {
        return adPlaybackState;
    }

    List<AdvertBreak> advertBreaks() {
        return advertBreaks;
    }

    private static List<AdvertBreak> sortAdvertBreaksByStartTime(List<AdvertBreak> advertBreaks) {
        List<AdvertBreak> sortedAdvertBreaks = new ArrayList<>(advertBreaks);
        Collections.sort(sortedAdvertBreaks, new AdvertBreakStartTimeComparator());
        return sortedAdvertBreaks;
    }

    private static long[] advertBreakOffset(List<AdvertBreak> advertBreaks) {
        long[] advertOffsets = new long[advertBreaks.size()];
        for (int i = 0; i < advertOffsets.length; i++) {
            advertOffsets[i] = C.msToUs(advertBreaks.get(i).startTimeInMillis());
        }
        return advertOffsets;
    }

}

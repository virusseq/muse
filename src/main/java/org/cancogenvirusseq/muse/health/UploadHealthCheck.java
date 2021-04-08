package org.cancogenvirusseq.muse.health;

import org.cancogenvirusseq.muse.service.SongScoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class UploadHealthCheck implements HealthIndicator {

  private static final String MESSAGE_KEY = "uploadDisposable";

  private final SongScoreService songScoreService;

  @Autowired
  public UploadHealthCheck(SongScoreService songScoreService) {
    this.songScoreService = songScoreService;
  }

  @Override
  public Health health() {
    if (!songScoreService.getSubmitUploadDisposable().isDisposed()) {
      return Health.up()
          .withDetail(MESSAGE_KEY, "Upload disposable is running.")
          .build(); // Healthy
    }
    return Health.down()
        .withDetail(MESSAGE_KEY, "Upload disposable has stopped.")
        .build(); // Unhealthy
  }
}

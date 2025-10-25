package com.github.javydreamercsw.management.event;

import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class FanChangeEventListener implements ApplicationListener<FanAwardedEvent> {

  @Override
  public void onApplicationEvent(FanAwardedEvent event) {
    FanChangeBroadcaster.broadcast(event);
  }
}

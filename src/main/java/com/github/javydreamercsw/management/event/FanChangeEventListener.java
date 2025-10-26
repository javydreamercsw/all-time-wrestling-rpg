package com.github.javydreamercsw.management.event;

import com.github.javydreamercsw.base.event.FanAwardedEvent;
import com.github.javydreamercsw.base.event.FanChangeBroadcaster;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class FanChangeEventListener implements ApplicationListener<FanAwardedEvent> {

  @Override
  public void onApplicationEvent(FanAwardedEvent event) {
    FanChangeBroadcaster.broadcast(event);
  }
}

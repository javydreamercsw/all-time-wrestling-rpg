package com.github.javydreamercsw.base.ui.view;

import com.vaadin.flow.component.icon.VaadinIcon;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MenuItem {
  private String title;
  private VaadinIcon icon;
  private String path;
  private List<MenuItem> children = new ArrayList<>();

  public MenuItem(String title, VaadinIcon icon, String path) {
    this.title = title;
    this.icon = icon;
    this.path = path;
  }

  public void addChild(MenuItem child) {
    children.add(child);
  }
}

# Task List

- [x] Fix the two failing tests in `src/test/java/com/github/javydreamercsw/management/ui/view/show/EditSegmentDialogTest.java`.
- [x] Fix the failing test in `src/test/java/com/github/javydreamercsw/management/ui/view/ranking/RankingViewTest.java`.
- [ ] Add segment titles to the SegmentNarrationContext along with instructions on how to use them in the narration of the segment.
- [ ] Enhance E2E testing. Interacting with Vaadin components via Selenium can be challenging because Vaadin abstracts much of the DOM and uses Shadow DOM, making standard Selenium selectors less effective. The paid Vaadin TestBench provides deep integration, but for open-source projects, you can still improve your Selenium tests by:

- Using JavaScript execution (`JavascriptExecutor`) to access elements inside Shadow DOM.
- Targeting component IDs or custom attributes you add for testing (e.g., `data-testid`).
- Leveraging Vaadin’s [TestBench Element API](https://vaadin.com/docs/latest/testbench/element-api/) concepts in your own helper methods.

**Example: Accessing a Vaadin Button inside Shadow DOM**
```java
// Java (Selenium)
WebElement vaadinButton = (WebElement) ((JavascriptExecutor) driver)
    .executeScript("return document.querySelector('vaadin-button').shadowRoot.querySelector('button')");
vaadinButton.click();
```

**Tip:** Add unique `id` or `data-testid` attributes to your Vaadin components to simplify selection.

While not as seamless as TestBench, these techniques allow you to write more robust Selenium tests for Vaadin apps without extra cost.
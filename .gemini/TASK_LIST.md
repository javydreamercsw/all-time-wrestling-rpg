# Task List
- [x] Implement show segment ordering and main event.
- [ ] UI enhancements
    - [x] In the UI, Title view, there no way to set no challenger once a value is selected. Add an option to clear the challenger selection.
    - [x] Fan Management
        - [ ] Add functionality to manage fan interactions and engagement within the application. We need ways of adding/removing fans to a wrestler.
        - [ ] When a match for a championship takes place, challenger(s) should pay the appropriate fan fee triggering a fan adjudication event.
           [ ] Implement fan fee payment processing. See table below:
            
            | Name | #1 Contender Match fee | Title Match fee |
            |---|---|---|
            | ATW World  | 35,000  | 100,000  |
            | ATW Intertemporal  | 25,000  | 60,000  |
            |  ATW Tag Team | 20,000  | 40,000  |
             |ATW Extreme | 15,000  |  25,000 |

          - [ ] Create fan adjudication event logic.
          - [ ] Update UI to reflect fan interactions and adjudications.
    - [ ] Event Enhancements
        - [ ] Add events for the following actions:
            - [ ] Heat changes (increase, resolution, etc.)
            - [ ] Championship defenses
            - [ ] Championship holder changes
        - [ ] Add an inbox area where the different events are stored for viewing later.
            - [ ] Allow users to mark events as read/unread.
            - [ ] Implement filtering options to view events by type, date, or read status.
            - [ ] Add a search functionality to find specific events quickly.
            - [ ] Improve the visual design of the event list for better user experience.
    - [x] Add features to allow ordering segment within a show and setting which match segment is the main event.
        - [x] Improve the narration context so AI is aware of the order of segments and which is the main event. This to allow for better narration generation taking into account events that took place earlier in the show.
        - [x] Update the UI to allow users to set the order of segments and mark the main event.
        - [x] Update tests to cover the new functionality.
        - [x] Update documentation to explain how to use the new segment ordering and main event features.
        - [x] Refactor the existing code to improve maintainability and readability.
- [ ] Enhance E2E testing. See tests like ShowDetailViewTest that use Karibu Testing successfully.
    - [ ] Identify key user flows that need to be tested end-to-end.
    - [ ] Write E2E tests for these user flows using Karibu Testing.
    - [ ] Ensure tests cover various scenarios, including edge cases.
    - [ ] Integrate E2E tests into the CI/CD pipeline for automated testing.
    - [ ] Document the E2E testing process and how to run the tests.
- [ ] Improve code coverage for the project.
    - [ ] Identify areas of the codebase with low test coverage.
    - [ ] Write unit tests to cover these areas.
    - [ ] Use code coverage tools to measure improvements.
    - [ ] Set coverage goals and track progress over time.
    - [ ] Review and refactor existing tests to ensure they are effective and efficient.
- [ ] Update project documentation to reflect recent changes and new features.
    - [ ] Review existing documentation for accuracy and completeness.
    - [ ] Add documentation for new features and changes.
    - [ ] Ensure documentation is clear and easy to understand.
    - [ ] Use diagrams and examples where appropriate to enhance understanding.
    - [ ] Regularly update documentation as the project evolves.
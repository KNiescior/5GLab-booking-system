# Merge conflict resolution for test files

When merging (e.g. `main` into your branch), these files may show conflicts:

- `src/test/java/com/_glab/booking_system/booking/LabManagerReservationIntegrationTest.java`
- `src/test/java/com/_glab/booking_system/booking/ProfessorReservationEditIntegrationTest.java`
- `src/test/java/com/_glab/booking_system/booking/service/ReservationEditServiceTest.java`

## Steps (command line)

1. **Start the merge** (from repo root):
   ```bash
   git fetch origin
   git merge origin/main
   ```
   Or, if you're merging another branch: `git merge <other-branch>`.

2. **Open each conflicted file** and look for conflict markers:
   - `<<<<<<< HEAD` (your branch)
   - `=======` (separator)
   - `>>>>>>> origin/main` (incoming branch)

3. **Resolve each conflict** by choosing one version or combining both:
   - **LabManagerReservationIntegrationTest** and **ProfessorReservationEditIntegrationTest**: Keep the version that includes:
     - `@Autowired` for: `BuildingOperatingHoursRepository`, `BuildingClosedDayRepository`, `LabOperatingHoursRepository`, `LabClosedDayRepository`, `SpecialOperatingHoursRepository`
     - Cleanup order in `@BeforeEach`: delete `specialOperatingHoursRepository`, `labClosedDayRepository`, `labOperatingHoursRepository` before `labRepository`, and `buildingClosedDayRepository`, `buildingOperatingHoursRepository` before `buildingRepository`.
   - **ReservationEditServiceTest**: Keep the version that uses:
     - `EditAlreadyResolvedException` (not `IllegalStateException`) for "edit proposal already exists"
     - `InvalidEditException` (not `IllegalStateException`) for "non-editable status", "not created by a lab manager", and "not created by the reservation owner"
     - Imports for `EditAlreadyResolvedException` and `InvalidEditException`

4. **Remove all conflict markers** (`<<<<<<<`, `=======`, `>>>>>>>`) from the file.

5. **Stage resolved files and finish**:
   ```bash
   git add src/test/java/com/_glab/booking_system/booking/LabManagerReservationIntegrationTest.java
   git add src/test/java/com/_glab/booking_system/booking/ProfessorReservationEditIntegrationTest.java
   git add src/test/java/com/_glab/booking_system/booking/service/ReservationEditServiceTest.java
   git status
   git commit -m "Resolve merge conflicts in integration and edit service tests"
   ```

## Accepting “ours” or “theirs” for a single file

- To keep **your branch** version for one file:
  ```bash
  git checkout --ours -- path/to/file.java
  git add path/to/file.java
  ```
- To keep **incoming branch** version:
  ```bash
  git checkout --theirs -- path/to/file.java
  git add path/to/file.java
  ```

Use “ours” if your branch has the new FK cleanup and exception fixes; use “theirs” only if the other branch has those changes and you want to drop yours.

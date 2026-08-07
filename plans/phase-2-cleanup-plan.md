# Phase 2: Remove Fake Functions & Duplicates

## Status: 🔄 IN PROGRESS

## Objective
Remove placeholder buttons, Toast-only features, and ensure all UI elements have real functionality or are removed.

---

## Findings

### 1. SettingsScreen.kt - File Management Section

**Location**: `app/src/main/java/com/example/feature/settings/SettingsScreen.kt` (lines 280-340)

**Toast-Only Buttons Identified**:
1. ✅ **"Baru" (New)** - Shows Toast, no real functionality
2. ✅ **"Buka" (Open)** - Shows Toast, no real functionality
3. ✅ **"Penyimpanan" (Storage)** - Shows Toast, no real functionality
4. ✅ **"Terbaru" (Recent)** - Shows Toast, no real functionality
5. ✅ **"Simpan" (Save)** - Shows Toast, no real functionality
6. ✅ **"Simpan sbg" (Save as)** - Shows Toast, no real functionality
7. ✅ **"Bagikan" (Share)** - Shows Toast, no real functionality
8. ✅ **"Cetak" (Print)** - Shows Toast, no real functionality

**Analysis**:
- These buttons are in the "File Management" (Manajemen File) card
- They appear to be placeholders for future project management features
- Currently provide no value to users - just show Toast messages
- Not connected to any actual file operations

**Recommendation**: 
**REMOVE** the entire "File Management" card section. The app already has:
- Project Manager screen for managing projects
- Export functionality in each tool (Loop, Editor, Mastering, etc.)
- History screen for viewing past renders
- Settings for output directory configuration

These buttons duplicate functionality that exists elsewhere or represent features that aren't implemented.

---

## Additional Areas to Audit

### 2. Navigation Routes
- [ ] Verify all navigation destinations have real screens
- [ ] Check for any dead-end routes
- [ ] Test back navigation from all screens

### 3. Spectrum Data
- [ ] Verify Editor uses real audio analysis (not dummy data)
- [ ] Check Visualizer uses real FFT analysis
- [ ] Confirm Mastering uses actual audio metrics

### 4. Language Consistency
- [ ] Audit UI strings for mixed Indonesian/English
- [ ] Standardize to single language (Indonesian preferred based on existing UI)

### 5. Repository Structure
- [ ] Review root-level patch scripts (should be in scripts/archive/)
- [ ] Clean up any temporary test files
- [ ] Verify .gitignore is comprehensive

---

## Action Plan

### Step 1: Remove File Management Card ✅ READY
**File**: `app/src/main/java/com/example/feature/settings/SettingsScreen.kt`

**Action**: Delete lines 280-340 (entire "File Management Card" section)

**Justification**:
- No real functionality implemented
- Duplicates existing features (Project Manager, Export dialogs)
- Confuses users with non-functional buttons
- Reduces code maintenance burden

### Step 2: Verify Navigation Routes
**Files to check**:
- `app/src/main/java/com/example/navigation/NavGraph.kt`
- All Screen composables

**Action**: 
- List all navigation routes
- Verify each route has a real destination
- Test back navigation

### Step 3: Audit Spectrum/Audio Analysis
**Files to check**:
- `app/src/main/java/com/example/feature/editor/EditorViewModel.kt`
- `app/src/main/java/com/example/feature/visualizer/VisualizerViewModel.kt`
- `app/src/main/java/com/example/core/media/Media3SpectrumAudioProcessor.kt`

**Action**:
- Search for "dummy", "mock", "fake" in audio processing code
- Verify real FFT analysis is used
- Confirm no hardcoded test data

### Step 4: Language Consistency Audit
**Action**:
- Search for mixed language strings
- Standardize to Indonesian (current UI language)
- Update string resources if needed

### Step 5: Repository Cleanup
**Action**:
- Move any root-level scripts to `scripts/archive/`
- Remove temporary test files
- Update .gitignore if needed

---

## Success Criteria

- [ ] No Toast-only buttons in production UI
- [ ] All navigation routes lead to real screens
- [ ] Back navigation works from all screens
- [ ] No dummy/mock data in audio processing
- [ ] Consistent UI language throughout app
- [ ] Clean repository structure
- [ ] All changes committed with clear message

---

## Estimated Time
- Step 1 (Remove File Management): 15 minutes
- Step 2 (Navigation audit): 30 minutes
- Step 3 (Audio analysis audit): 45 minutes
- Step 4 (Language audit): 30 minutes
- Step 5 (Repository cleanup): 20 minutes

**Total**: ~2.5 hours

---

## Next Steps

1. Switch to 'code' mode
2. Remove File Management card from SettingsScreen.kt
3. Continue with remaining audit steps
4. Commit changes: "fix(ui): remove placeholder file management buttons"
5. Proceed to Phase 3

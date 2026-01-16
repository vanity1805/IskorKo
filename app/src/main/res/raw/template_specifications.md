# IskorKo Answer Sheet Template Specifications

## Overview
These specifications are designed to maximize OMR (Optical Mark Recognition) accuracy.

---

## 20-Question Template (2 columns × 10 rows)

### Page Layout
- **Paper Size**: Letter (8.5" × 11") or A4
- **Orientation**: Portrait
- **Margins**: 0.5" on all sides

### Corner Timing Marks (REQUIRED)
- **Size**: 0.3" × 0.3" (8mm × 8mm) solid black squares
- **Position**: All 4 corners, 0.25" from edge
- **Purpose**: Paper detection and perspective correction

### Row Timing Marks (NEW - REQUIRED)
- **Size**: 0.15" × 0.15" (4mm × 4mm) solid black squares
- **Position**: Left edge of paper, aligned with each question row
- **Spacing**: Must align exactly with the vertical center of each bubble row
- **Purpose**: Precise row detection

```
Layout Example (20 questions):

[■]  IskorKo Answer Sheet                              [■]
      Automated Grading System
      
  ┌─────────────────────────────────────────────────────┐
  │ NAME:                           │ DATE:             │
  ├─────────────────────────────────┼───────────────────┤
  │ STUDENT ID:                     │ PERIOD/CLASS:     │
  └─────────────────────────────────┴───────────────────┘
  
     Column 1 (Q1-10)              Column 2 (Q11-20)
     A   B   C   D   E              A   B   C   D   E
  ■  ○   ○   ○   ○   ○    1.    ■  ○   ○   ○   ○   ○   11.
  ■  ○   ○   ○   ○   ○    2.    ■  ○   ○   ○   ○   ○   12.
  ■  ○   ○   ○   ○   ○    3.    ■  ○   ○   ○   ○   ○   13.
  ■  ○   ○   ○   ○   ○    4.    ■  ○   ○   ○   ○   ○   14.
  ■  ○   ○   ○   ○   ○    5.    ■  ○   ○   ○   ○   ○   15.
  ■  ○   ○   ○   ○   ○    6.    ■  ○   ○   ○   ○   ○   16.
  ■  ○   ○   ○   ○   ○    7.    ■  ○   ○   ○   ○   ○   17.
  ■  ○   ○   ○   ○   ○    8.    ■  ○   ○   ○   ○   ○   18.
  ■  ○   ○   ○   ○   ○    9.    ■  ○   ○   ○   ○   ○   19.
  ■  ○   ○   ○   ○   ○   10.    ■  ○   ○   ○   ○   ○   20.

[■]  INSTRUCTIONS                                       [■]
     • Use #2 pencil or dark pen
     • Fill bubbles completely
     • Keep all corner marks visible

Legend:
[■] = Corner timing mark (0.3" × 0.3")
■   = Row timing mark (0.15" × 0.15")
○   = Answer bubble
```

### Bubble Specifications
- **Size**: 0.25" diameter (6mm)
- **Spacing**: 0.35" center-to-center horizontally
- **Row Height**: 0.4" center-to-center vertically
- **Style**: Hollow circle with thin black outline (1pt stroke)
- **Fill Area**: Students shade inside the circle

### Column Layout
- **Number of Columns**: 2 (for 20 questions)
- **Column 1**: Questions 1-10 (left side)
- **Column 2**: Questions 11-20 (right side)
- **Column Gap**: At least 0.75" between columns

### Row Timing Mark Placement (Critical!)
Each row timing mark must be:
1. **Aligned**: Vertically centered with its corresponding bubble row
2. **Positioned**: 0.1" to the LEFT of the first bubble (A)
3. **Consistent**: Same vertical position for each question

---

## 50-Question Template (3 columns × 17 rows)

```
Layout:
Column 1: Q1-17   (17 rows)
Column 2: Q18-34  (17 rows)  
Column 3: Q35-50  (16 rows + 1 blank)

Row timing marks on left edge of EACH column
```

---

## 100-Question Template (4 columns × 25 rows)

```
Layout:
Column 1: Q1-25   (25 rows)
Column 2: Q26-50  (25 rows)
Column 3: Q51-75  (25 rows)
Column 4: Q76-100 (25 rows)

Row timing marks on left edge of EACH column
```

---

## Design Tips for Accurate Scanning

### DO:
✅ Use solid BLACK for all timing marks
✅ Keep timing marks same size throughout
✅ Maintain consistent spacing between rows
✅ Use thin outlines for bubbles (not thick)
✅ Leave adequate white space around timing marks
✅ Print on WHITE paper only

### DON'T:
❌ Use gray or colored timing marks
❌ Place text too close to timing marks
❌ Make bubbles too small (< 5mm)
❌ Use decorative borders that could be detected as marks
❌ Print on colored or textured paper

---

## Printing Requirements
- **DPI**: Minimum 300 DPI
- **Paper**: White, 20-24 lb bond
- **Ink**: Black, high contrast
- **Alignment**: Center on page

---

## Scanner Expectations

The IskorKo scanner will:
1. Detect 4 corner marks → Perspective correction
2. Detect row timing marks → Locate each question row
3. Detect bubbles in each row → Find 5 options (A-E)
4. Analyze darkness → Determine filled bubble

With proper timing marks, accuracy should be >95%

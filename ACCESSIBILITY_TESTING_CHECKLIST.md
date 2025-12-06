# Accessibility Testing Checklist

This document provides a comprehensive checklist for testing the accessibility features of the Android Remote Notify app.

## Prerequisites

Before starting accessibility testing, ensure you have:
- [ ] A physical Android device or emulator running Android 11 (API 30) or higher
- [ ] TalkBack screen reader enabled on the device
- [ ] Accessibility Scanner app installed (optional but recommended)
- [ ] Font size set to various levels (100%, 150%, 200%) for testing

## TalkBack Navigation Testing

### Main Screens

#### Alerts List Screen
- [ ] Navigate to Alerts List Screen using TalkBack
- [ ] Verify "About App" button is announced correctly
- [ ] Verify "Settings" button is announced correctly
- [ ] Verify "Add Alert" floating action button is announced clearly
- [ ] Verify battery level card announces "Battery level at X percent"
- [ ] Verify storage card announces "Storage available X of Y gigabytes"
- [ ] Verify device status card is properly merged and readable
- [ ] Test navigation through empty alerts state
- [ ] Test navigation through configured alerts list
- [ ] Verify each alert item is selectable and announces alert type
- [ ] Verify delete button announces correct alert type being deleted
- [ ] Test "Learn More" button in empty state

#### Notification Medium List Screen
- [ ] Navigate to screen and verify title is read
- [ ] Verify back button announces "Navigate back"
- [ ] Navigate through each notification medium card
- [ ] Verify each medium's icon and name is properly announced
- [ ] Verify configuration status (Configured/Not Configured) is read
- [ ] Test edit/configure button announces medium name
- [ ] Test reset button announces medium name when visible
- [ ] Navigate to check frequency slider
- [ ] Verify slider value changes are announced
- [ ] Test feedback section is accessible

#### Add/Edit Alert Screen
- [ ] Navigate to "Add New Alert" screen
- [ ] Verify back button is announced
- [ ] Navigate through alert type selector buttons
- [ ] Verify battery and storage icons are identified
- [ ] Navigate to threshold slider
- [ ] Verify slider changes are announced with values
- [ ] Test preview card is readable with merged content
- [ ] Verify save/update button is clearly announced
- [ ] Test battery optimization card if shown

#### Alert Check Log Viewer Screen
- [ ] Navigate to logs screen
- [ ] Verify back button announces correctly
- [ ] Test filter button with and without active filters
- [ ] Verify badge announces filter status
- [ ] Navigate through log items
- [ ] Test expandable log details
- [ ] Verify date and time information is read correctly
- [ ] Test clear filters button

## Content Descriptions Audit

### Icons with Content Descriptions
- [ ] Battery icon: "Battery level at X percent"
- [ ] Storage icon: "Storage available X of Y gigabytes"
- [ ] Battery alert icon: "Battery alert icon"
- [ ] Storage alert icon: "Storage alert icon"
- [ ] Delete button: "Delete battery/storage alert"
- [ ] Pending check icon: "Pending check"
- [ ] Schedule icon: "Worker schedule"
- [ ] No alerts icon: "No alerts configured"
- [ ] Notification medium icons: "[Medium name] notification medium"
- [ ] Settings icon: "Configure [medium name]"
- [ ] Reset icon: "Reset [medium name] configuration"
- [ ] Refresh icon: "Check frequency configuration"
- [ ] Filter icon: "Filter logs" or "Filter logs (filters active)"
- [ ] More options icon: "More options"

### Buttons and Interactive Elements
- [ ] All IconButtons have meaningful content descriptions
- [ ] All navigation buttons announce "Navigate back"
- [ ] All action buttons clearly state their purpose
- [ ] Segmented buttons rely on text labels (icons set to null)
- [ ] Menu items with icons set icon descriptions to null (text provides context)

## Semantic Labels Testing

### Complex UI Components
- [ ] Device status card merges battery and storage info into single announcement
- [ ] Alert item cards have appropriate semantic structure
- [ ] Notification medium cards group related information
- [ ] Log items merge multiple data points appropriately

## Font Size Scaling

### Test with Different Font Sizes
- [ ] 100% (default) - All text is readable
- [ ] 150% - Text scales properly without clipping
- [ ] 200% - UI remains functional and readable
- [ ] No text truncation occurs at any scale
- [ ] All interactive elements remain accessible

### Screens to Test at Each Size
- [ ] Alerts List Screen
- [ ] Notification Medium List Screen
- [ ] Add/Edit Alert Screen
- [ ] Alert Check Log Viewer Screen
- [ ] Configuration screens for each medium

## Touch Target Sizes

### Minimum Touch Target Verification (48dp × 48dp)
- [ ] All IconButtons meet minimum size
- [ ] Floating Action Button is appropriately sized
- [ ] List item touch targets are sufficient
- [ ] Slider thumb is large enough to manipulate
- [ ] Filter chips are appropriately sized
- [ ] All buttons meet minimum touch target

## Contrast Ratios (WCAG AA)

### Visual Verification
- [ ] Primary text has 4.5:1 contrast ratio
- [ ] Secondary text has 4.5:1 contrast ratio
- [ ] Error text (low battery indicator) is distinguishable
- [ ] Icon colors meet contrast requirements
- [ ] Button text is readable in both light and dark modes
- [ ] Disabled state has appropriate visual feedback

### Test Both Themes
- [ ] Light theme passes all contrast checks
- [ ] Dark theme passes all contrast checks

## Focus Order

### Navigation Flow Testing
- [ ] Logical reading order on Alerts List Screen (top to bottom)
- [ ] Settings and configuration screens follow natural flow
- [ ] Form fields in Add Alert screen follow logical order
- [ ] Filter options are in sensible order
- [ ] No focus traps or inaccessible areas
- [ ] Tab navigation (if using keyboard) follows visual order

## Accessibility Scanner Results

### Run Accessibility Scanner on Each Screen
- [ ] Alerts List Screen - No critical issues
- [ ] Notification Medium List Screen - No critical issues
- [ ] Add/Edit Alert Screen - No critical issues
- [ ] Alert Check Log Viewer Screen - No critical issues
- [ ] Configuration screens - No critical issues

### Address Scanner Recommendations
- [ ] Review and address any content description suggestions
- [ ] Review and address any touch target size warnings
- [ ] Review and address any contrast ratio warnings
- [ ] Review and address any text sizing suggestions

## High Contrast Mode

### Test with System High Contrast Enabled
- [ ] All UI elements remain visible
- [ ] Text is clearly readable
- [ ] Icons are distinguishable
- [ ] Interactive elements are identifiable
- [ ] No information is lost in high contrast mode

## Voice Input Testing

### Test Voice Input for Text Fields
- [ ] Configuration screens accept voice input for URLs/emails
- [ ] Voice input works for all text fields
- [ ] Voice commands can activate buttons (if supported)

## State Announcements

### Dynamic Content Updates
- [ ] Slider value changes are announced
- [ ] Filter application announces results
- [ ] Success/error states are announced
- [ ] Loading states are communicated
- [ ] Empty states provide clear guidance

## Edge Cases

### Special Scenarios
- [ ] App behaves correctly with TalkBack during first launch
- [ ] Education/tutorial sheets are accessible
- [ ] Bottom sheets can be dismissed accessibly
- [ ] Dialogs announce their purpose
- [ ] Snackbars are read by TalkBack
- [ ] Long-press actions have alternatives

## Material Design Guidelines Compliance

### Material 3 Accessibility Features
- [ ] Uses Material3 components with built-in accessibility
- [ ] Typography scales properly with system settings
- [ ] Touch ripples provide visual feedback
- [ ] State layers indicate interactive elements
- [ ] Elevation and shadows don't impact readability

## Testing Notes

### Device/Emulator Used
- Device Model: _________________
- Android Version: ______________
- TalkBack Version: _____________
- Date Tested: _________________

### Issues Found
| Screen | Issue | Severity | Status |
|--------|-------|----------|--------|
|        |       |          |        |

### Recommendations
_Add any suggestions for future accessibility improvements here_

---

## Resources

- [Android Accessibility Guide](https://developer.android.com/guide/topics/ui/accessibility)
- [Compose Accessibility Documentation](https://developer.android.com/jetpack/compose/accessibility)
- [Material Design Accessibility](https://m3.material.io/foundations/accessible-design/overview)
- [WCAG 2.1 Guidelines](https://www.w3.org/WAI/WCAG21/quickref/)
- [Accessibility Scanner App](https://play.google.com/store/apps/details?id=com.google.android.apps.accessibility.auditor)

## Completion Status

- [ ] All tests completed
- [ ] All critical issues resolved
- [ ] Documentation updated
- [ ] Team notified of results

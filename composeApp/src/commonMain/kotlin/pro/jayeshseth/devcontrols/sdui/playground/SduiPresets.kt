package pro.jayeshseth.devcontrols.sdui.playground

/**
 * Curated showcase of Server-Driven UI JSON schemas demonstrating:
 * 1. Declarative AST hierarchy
 * 2. Dynamic expression engine & math/ternary logic
 * 3. 60/120fps Native Compose animations (springs, tweens, gestures)
 * 4. Responsive multiplatform layout modifiers
 */
object SduiPresets {

    val counterPreset = """
{
  "id": "spring_counter",
  "version": 1,
  "metadata": {
    "title": "Spring Physics & Animated Counter"
  },
  "initialState": {
    "count": 0,
    "scale": 1.0,
    "isAwesome": false
  },
  "root": {
    "type": "column",
    "spacingDp": 16.0,
    "horizontalAlignment": "centerHorizontally",
    "modifiers": [
      { "type": "padding", "all": "20" },
      { "type": "size", "fillMaxWidth": "1.0" }
    ],
    "children": [
      {
        "type": "card",
        "cornerRadius": 20.0,
        "elevation": 8.0,
        "backgroundColor": "${'$'}{count % 2 == 0 ? '#1C1B1F' : '#2D1B36'}",
        "modifiers": [
          { "type": "padding", "all": "8" },
          { "type": "size", "fillMaxWidth": "1.0" },
          {
            "type": "graphicsLayer",
            "scaleX": "${'$'}{scale}",
            "scaleY": "${'$'}{scale}"
          }
        ],
        "children": [
          {
            "type": "column",
            "spacingDp": 12.0,
            "horizontalAlignment": "centerHorizontally",
            "modifiers": [
              { "type": "padding", "all": "24" },
              { "type": "size", "fillMaxWidth": "1.0" }
            ],
            "children": [
              {
                "type": "image",
                "vectorIcon": "star",
                "tintColor": "${'$'}{count > 5 ? '#FFD700' : '#888888'}",
                "modifiers": [
                  { "type": "size", "width": "48", "height": "48" }
                ]
              },
              {
                "type": "text",
                "text": "Score: ${'$'}{count * 10} pts",
                "fontSize": 26.0,
                "fontWeight": "bold",
                "color": "#FFFFFF"
              },
              {
                "type": "text",
                "text": "${'$'}{count == 0 ? 'Tap Tap to Start!' : (count >= 10 ? 'Unstoppable Legend!' : 'Keep going!')}",
                "fontSize": 14.0,
                "fontWeight": "medium",
                "color": "#BB86FC"
              }
            ]
          }
        ]
      },
      {
        "type": "row",
        "spacingDp": 12.0,
        "horizontalArrangement": "center",
        "children": [
          {
            "type": "button",
            "text": "+ Tap Count",
            "variant": "filled",
            "onClick": [
              { "type": "setState", "key": "count", "valueExpr": "count + 1" },
              { "type": "animateTo", "key": "scale", "targetValueExpr": "1.2", "animation": { "type": "spring", "dampingRatio": "highBouncy", "stiffness": "high" } },
              { "type": "animateTo", "key": "scale", "targetValueExpr": "1.0", "animation": { "type": "spring", "dampingRatio": "lowBouncy", "stiffness": "medium" } }
            ]
          },
          {
            "type": "button",
            "text": "Reset",
            "variant": "outlined",
            "onClick": [
              { "type": "setState", "key": "count", "valueExpr": "0" },
              { "type": "animateTo", "key": "scale", "targetValueExpr": "1.0" }
            ]
          }
        ]
      },
      {
        "type": "button",
        "text": "${'$'}{isAwesome ? 'Hide Secret Message' : 'Reveal Secret'}",
        "variant": "text",
        "onClick": [
          { "type": "toggleState", "key": "isAwesome" }
        ]
      },
      {
        "type": "animatedVisibility",
        "visibleExpr": "isAwesome",
        "enter": { "fadeIn": true, "slideInVertically": true, "slideVerticalDirection": "down", "expandVertically": true },
        "exit": { "fadeOut": true, "shrinkVertically": true },
        "child": {
          "type": "card",
          "cornerRadius": 12.0,
          "elevation": 2.0,
          "backgroundColor": "#004D40",
          "modifiers": [
            { "type": "padding", "all": "8" },
            { "type": "size", "fillMaxWidth": "1.0" }
          ],
          "children": [
            {
              "type": "text",
              "text": "Server-driven animations run at native Compose 60/120fps!",
              "color": "#80CBC4",
              "fontSize": 14.0,
              "fontWeight": "semiBold",
              "textAlign": "center",
              "modifiers": [{ "type": "padding", "all": "16" }]
            }
          ]
        }
      }
    ]
  }
}
""".trimIndent()

    val gestureDragPreset = """
{
  "id": "gesture_drag_card",
  "version": 1,
  "metadata": {
    "title": "Gesture Physics Drag Card"
  },
  "initialState": {
    "dragOffset": 0.0,
    "dismissed": false
  },
  "root": {
    "type": "column",
    "spacingDp": 20.0,
    "horizontalAlignment": "centerHorizontally",
    "modifiers": [
      { "type": "padding", "all": "20" },
      { "type": "size", "fillMaxWidth": "1.0" }
    ],
    "children": [
      {
        "type": "text",
        "text": "Drag the Card Horizontally",
        "fontSize": 18.0,
        "fontWeight": "bold"
      },
      {
        "type": "card",
        "cornerRadius": 16.0,
        "elevation": 6.0,
        "backgroundColor": "${'$'}{dragOffset > 60 ? '#1B5E20' : (dragOffset < -60 ? '#B71C1C' : '#263238')}",
        "modifiers": [
          { "type": "size", "fillMaxWidth": "1.0", "height": "160" },
          {
            "type": "gestureDrag",
            "orientation": "horizontal",
            "minOffset": -300.0,
            "maxOffset": 300.0,
            "targetStateKey": "dragOffset",
            "snapBack": true
          },
          {
            "type": "graphicsLayer",
            "translationX": "${'$'}{dragOffset}",
            "rotationZ": "${'$'}{dragOffset * 0.06}"
          }
        ],
        "children": [
          {
            "type": "column",
            "spacingDp": 8.0,
            "horizontalAlignment": "centerHorizontally",
            "verticalArrangement": "center",
            "modifiers": [
              { "type": "padding", "all": "16" },
              { "type": "size", "fillMaxSize": "1.0" }
            ],
            "children": [
              {
                "type": "text",
                "text": "${'$'}{dragOffset > 60 ? 'Accept / Like' : (dragOffset < -60 ? 'Dismiss / Reject' : 'Swipe Me Left or Right')}",
                "fontSize": 18.0,
                "fontWeight": "bold",
                "color": "#FFFFFF"
              },
              {
                "type": "text",
                "text": "Offset: ${'$'}{round(dragOffset)} px",
                "fontSize": 14.0,
                "color": "#B0BEC5"
              }
            ]
          }
        ]
      },
      {
        "type": "button",
        "text": "Spring Center",
        "variant": "outlined",
        "onClick": [
          {
            "type": "animateTo",
            "key": "dragOffset",
            "targetValueExpr": "0.0",
            "animation": { "type": "spring", "dampingRatio": "mediumBouncy", "stiffness": "medium" }
          }
        ]
      }
    ]
  }
}
""".trimIndent()

    val ecommercePreset = """
{
  "id": "ecommerce_product_card",
  "version": 1,
  "metadata": {
    "title": "Interactive E-Commerce Product Card"
  },
  "initialState": {
    "basePrice": 49.99,
    "qty": 1,
    "hasDiscount": false,
    "selectedSize": "M",
    "favorite": false
  },
  "root": {
    "type": "column",
    "spacingDp": 14.0,
    "modifiers": [
      { "type": "padding", "all": "16" },
      { "type": "size", "fillMaxWidth": "1.0" }
    ],
    "children": [
      {
        "type": "card",
        "cornerRadius": 16.0,
        "elevation": 4.0,
        "backgroundColor": "#1E1E24",
        "modifiers": [{ "type": "size", "fillMaxWidth": "1.0" }],
        "children": [
          {
            "type": "column",
            "spacingDp": 12.0,
            "modifiers": [{ "type": "padding", "all": "16" }],
            "children": [
              {
                "type": "row",
                "horizontalArrangement": "spaceBetween",
                "modifiers": [{ "type": "size", "fillMaxWidth": "1.0" }],
                "children": [
                  {
                    "type": "text",
                    "text": "Cyberpunk Neon Hoodie",
                    "fontSize": 20.0,
                    "fontWeight": "bold",
                    "color": "#FFFFFF"
                  },
                  {
                    "type": "image",
                    "vectorIcon": "heart",
                    "tintColor": "${'$'}{favorite ? '#FF1744' : '#757575'}",
                    "modifiers": [
                      { "type": "clickable", "actions": [{ "type": "toggleState", "key": "favorite" }] }
                    ]
                  }
                ]
              },
              {
                "type": "text",
                "text": "Total: ${'$'}${'$'}{round(basePrice * qty * (hasDiscount ? 0.8 : 1.0) * 100) / 100}",
                "fontSize": 22.0,
                "fontWeight": "bold",
                "color": "#00E676"
              },
              {
                "type": "row",
                "spacingDp": 8.0,
                "children": [
                  {
                    "type": "button",
                    "text": "-",
                    "variant": "outlined",
                    "enabledExpr": "qty > 1",
                    "onClick": [{ "type": "setState", "key": "qty", "valueExpr": "max(qty - 1, 1)" }]
                  },
                  {
                    "type": "text",
                    "text": "Qty: ${'$'}{qty}",
                    "fontSize": 16.0,
                    "fontWeight": "bold",
                    "color": "#FFFFFF",
                    "modifiers": [{ "type": "padding", "horizontal": "8", "vertical": "8" }]
                  },
                  {
                    "type": "button",
                    "text": "+",
                    "variant": "outlined",
                    "enabledExpr": "qty < 10",
                    "onClick": [{ "type": "setState", "key": "qty", "valueExpr": "min(qty + 1, 10)" }]
                  }
                ]
              },
              {
                "type": "row",
                "horizontalArrangement": "spaceBetween",
                "modifiers": [{ "type": "size", "fillMaxWidth": "1.0" }],
                "children": [
                  {
                    "type": "text",
                    "text": "20% Flash Coupon:",
                    "fontSize": 14.0,
                    "color": "#B0BEC5"
                  },
                  {
                    "type": "button",
                    "text": "${'$'}{hasDiscount ? 'Coupon Applied!' : 'Apply 20% Off'}",
                    "variant": "${'$'}{hasDiscount ? 'filled' : 'outlined'}",
                    "onClick": [{ "type": "toggleState", "key": "hasDiscount" }]
                  }
                ]
              }
            ]
          }
        ]
      }
    ]
  }
}
""".trimIndent()

    val canvasGraphicsPreset = """
{
  "id": "canvas_visualizer",
  "version": 1,
  "metadata": {
    "title": "Server-Driven Canvas Graphics"
  },
  "initialState": {
    "rotationAngle": 45.0,
    "arcSweep": 240.0
  },
  "root": {
    "type": "column",
    "spacingDp": 16.0,
    "horizontalAlignment": "centerHorizontally",
    "modifiers": [
      { "type": "padding", "all": "20" },
      { "type": "size", "fillMaxWidth": "1.0" }
    ],
    "children": [
      {
        "type": "text",
        "text": "Declarative 2D Canvas Graphics",
        "fontSize": 20.0,
        "fontWeight": "bold"
      },
      {
        "type": "canvas",
        "modifiers": [
          { "type": "size", "width": "220", "height": "220" },
          { "type": "background", "color": "#121212", "cornerRadius": 16.0 },
          { "type": "border", "width": "2", "color": "#333333", "cornerRadius": "16" }
        ],
        "operations": [
          { "type": "circle", "color": "#1E88E5", "centerX": "110", "centerY": "110", "radius": "90", "isFill": false, "strokeWidth": 4.0 },
          { "type": "arc", "color": "#FF4081", "x": "30", "y": "30", "width": "160", "height": "160", "startAngle": "0", "sweepAngle": "${'$'}{arcSweep}", "useCenter": false, "strokeWidth": 8.0 },
          { "type": "rotate", "degrees": "${'$'}{rotationAngle}", "ops": [
            { "type": "rect", "color": "#00E676", "x": "95", "y": "95", "width": "30", "height": "30", "cornerRadius": 4.0 },
            { "type": "line", "color": "#FFD600", "startX": "110", "startY": "20", "endX": "110", "endY": "200", "strokeWidth": 3.0 }
          ]}
        ]
      },
      {
        "type": "row",
        "spacingDp": 10.0,
        "children": [
          {
            "type": "button",
            "text": "Rotate +45°",
            "onClick": [
              {
                "type": "animateTo",
                "key": "rotationAngle",
                "targetValueExpr": "rotationAngle + 45.0",
                "animation": { "type": "spring", "dampingRatio": "mediumBouncy" }
              }
            ]
          },
          {
            "type": "button",
            "text": "Pulse Arc",
            "variant": "outlined",
            "onClick": [
              {
                "type": "animateTo",
                "key": "arcSweep",
                "targetValueExpr": "arcSweep > 180 ? 90.0 : 320.0",
                "animation": { "type": "tween", "durationMillis": 600, "easing": "fastOutSlowIn" }
              }
            ]
          }
        ]
      }
    ]
  }
}
""".trimIndent()

    val formValidationPreset = """
{
  "id": "form_validation",
  "version": 1,
  "metadata": {
    "title": "Form with Live Expression Validation"
  },
  "initialState": {
    "email": "",
    "password": "",
    "submitted": false
  },
  "root": {
    "type": "column",
    "spacingDp": 16.0,
    "modifiers": [
      { "type": "padding", "all": "20" },
      { "type": "size", "fillMaxWidth": "1.0" }
    ],
    "children": [
      {
        "type": "text",
        "text": "Dynamic Form Engine",
        "fontSize": 20.0,
        "fontWeight": "bold"
      },
      {
        "type": "textField",
        "valueStateKey": "email",
        "label": "Email Address",
        "placeholder": "user@example.com",
        "keyboardType": "email",
        "modifiers": [{ "type": "size", "fillMaxWidth": "1.0" }]
      },
      {
        "type": "textField",
        "valueStateKey": "password",
        "label": "Password (min 6 chars)",
        "placeholder": "••••••••",
        "isPassword": true,
        "modifiers": [{ "type": "size", "fillMaxWidth": "1.0" }]
      },
      {
        "type": "text",
        "text": "${'$'}{!contains(email, '@') && len(email) > 0 ? 'Email must contain @' : (len(password) > 0 && len(password) < 6 ? 'Password too short' : 'Form Valid!')}",
        "fontSize": 13.0,
        "color": "${'$'}{contains(email, '@') && len(password) >= 6 ? '#00E676' : '#FF5252'}"
      },
      {
        "type": "button",
        "text": "Submit Form",
        "enabledExpr": "contains(email, '@') && len(password) >= 6",
        "onClick": [
          { "type": "setState", "key": "submitted", "valueExpr": "true" }
        ],
        "modifiers": [{ "type": "size", "fillMaxWidth": "1.0" }]
      },
      {
        "type": "animatedVisibility",
        "visibleExpr": "submitted",
        "enter": { "fadeIn": true, "expandVertically": true },
        "child": {
          "type": "card",
          "backgroundColor": "#1B5E20",
          "modifiers": [{ "type": "padding", "vertical": "8" }, { "type": "size", "fillMaxWidth": "1.0" }],
          "children": [
            {
              "type": "text",
              "text": "Success! Account registered for ${'$'}{email}",
              "color": "#FFFFFF",
              "fontSize": 14.0,
              "fontWeight": "bold",
              "textAlign": "center",
              "modifiers": [{ "type": "padding", "all": "16" }]
            }
          ]
        }
      }
    ]
  }
}
""".trimIndent()

    val bottomSheetPreset = """
{
  "id": "bottom_sheet_showcase",
  "version": 1,
  "metadata": {
    "title": "Server-Driven Modal Bottom Sheet"
  },
  "initialState": {
    "isBottomSheetOpen": false,
    "delivery_sheet_isOpen": false,
    "deliveryOption": "Standard (Free)"
  },
  "root": {
    "type": "column",
    "spacingDp": 16.0,
    "modifiers": [
      { "type": "padding", "all": "20" },
      { "type": "size", "fillMaxWidth": "1.0" }
    ],
    "children": [
      {
        "type": "text",
        "text": "Server-Driven Bottom Sheet",
        "fontSize": 22.0,
        "fontWeight": "bold"
      },
      {
        "type": "text",
        "text": "Selected Delivery: ${'$'}{deliveryOption}",
        "fontSize": 16.0,
        "color": "#64B5F6"
      },
      {
        "type": "button",
        "text": "Choose Delivery Option",
        "onClick": [
          { "type": "openBottomSheet", "sheetId": "delivery_sheet" }
        ],
        "modifiers": [
          { "type": "size", "fillMaxWidth": "1.0" }
        ]
      }
    ]
  },
  "bottomSheet": {
    "id": "delivery_sheet",
    "isOpenExpr": "isBottomSheetOpen || delivery_sheet_isOpen",
    "skipPartiallyExpanded": false,
    "dragHandleVisible": true,
    "cornerRadiusDp": 24.0,
    "onDismissActions": [
      { "type": "closeBottomSheet", "sheetId": "delivery_sheet" }
    ],
    "content": {
      "type": "column",
      "spacingDp": 12.0,
      "modifiers": [
        { "type": "padding", "all": "24" },
        { "type": "size", "fillMaxWidth": "1.0" }
      ],
      "children": [
        {
          "type": "text",
          "text": "Select Delivery Method",
          "fontSize": 20.0,
          "fontWeight": "bold"
        },
        {
          "type": "button",
          "text": "Standard Delivery (3-5 days) - Free",
          "variant": "outlined",
          "onClick": [
            { "type": "setState", "key": "deliveryOption", "valueExpr": "'Standard (Free)'" },
            { "type": "closeBottomSheet", "sheetId": "delivery_sheet" }
          ],
          "modifiers": [{ "type": "size", "fillMaxWidth": "1.0" }]
        },
        {
          "type": "button",
          "text": "Express Delivery (1-2 days) - $4.99",
          "variant": "outlined",
          "onClick": [
            { "type": "setState", "key": "deliveryOption", "valueExpr": "'Express ($4.99)'" },
            { "type": "closeBottomSheet", "sheetId": "delivery_sheet" }
          ],
          "modifiers": [{ "type": "size", "fillMaxWidth": "1.0" }]
        },
        {
          "type": "button",
          "text": "Overnight Priority - $14.99",
          "variant": "outlined",
          "onClick": [
            { "type": "setState", "key": "deliveryOption", "valueExpr": "'Overnight Priority ($14.99)'" },
            { "type": "closeBottomSheet", "sheetId": "delivery_sheet" }
          ],
          "modifiers": [{ "type": "size", "fillMaxWidth": "1.0" }]
        }
      ]
    }
  }
}
""".trimIndent()
}

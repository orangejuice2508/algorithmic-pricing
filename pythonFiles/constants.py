import plotly.graph_objects as go

config = dict({
    'scrollZoom': True,
    'displayModeBar': True,
    'editable': True,
    'toImageButtonOptions': {
        'format': 'png',
        'filename': 'plot',
        'height': 1080,
        'width': 1920,
        'scale': 1
    }
})
layout = go.Layout(
    font=dict(
        family="Serif",
        size=36
    )
)
colorscale_heatmap_r = [
    [
        0,
        "rgb(255,255,255)"
    ],
    [
        0.125,
        "rgb(255,210,194)"
    ],
    [
        0.25,
        "rgb(255,156,126)"
    ],
    [
        0.375,
        "rgb(255,105,71)"
    ],
    [
        0.5,
        "rgb(255,44,9)"
    ],
    [
        0.625,
        "rgb(255,109,0)"
    ],
    [
        0.75,
        "rgb(255,158,0)"
    ],
    [
        0.875,
        "rgb(255,212,0)"
    ],
    [
        1,
        "rgb(255,252,1)"
    ]
]
colorscale_all_black = [
    [
        0,
        "rgb(0,0,0)"
    ],
    [
        1,
        "rgb(0,0,0)"
    ]
]

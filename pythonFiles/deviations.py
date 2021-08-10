import time

import numpy as np
import pandas as pd
import plotly.graph_objects as go
import constants

# Specify treatment to be analyzed
treatments = ["SIM-P-2", "SIM-P-3", "SIM-Q-2", "SIM-Q-3", "SEQ-P-2", "SEQ-P-3", "SEQ-Q-2", "SEQ-Q-3"]

for treatment in treatments:
    if treatment[4] == "P":
        independent_variable_english = "Price"
        independent_variable_german = "Preis"
    elif treatment[4] == "Q":
        independent_variable_english = "Quantity"
        independent_variable_german = "Menge"
    else:
        raise Exception("Invalid treatment!")

    df = pd.read_csv("data/deviations/" + treatment + ".csv")
    nash_equilibrium = df.get(independent_variable_english + " of firm 1").get(3)

    fig = go.Figure(layout=go.Layout(
        xaxis=dict(title="Periode"), yaxis=dict(title=independent_variable_german)
    ))
    fig.add_trace(go.Scatter(
        x=df.get("Period"), y=df.get(independent_variable_english + " of firm 1"),
        mode="lines",
        name=independent_variable_german + " von Firma 1"
    ))
    fig.add_trace(go.Scatter(
        x=df.get("Period"), y=df.get(independent_variable_english + " of firm 2"),
        mode="lines",
        name=independent_variable_german + " von Firma 2"
    ))
    fig.add_annotation(
        x="t", y=nash_equilibrium,
        showarrow=True, arrowhead=2, arrowsize=3,
        text="Erzwungene Abweichung",
        font_size=50
    )
    if len(df.columns) > 7:
        fig.add_trace(go.Scatter(
            x=df.get("Period"), y=df.get(independent_variable_english + " of firm 3"),
            mode="lines",
            name=independent_variable_german + " von Firma 3"
        ))
    y = np.empty(df.get("Period").size); y.fill(nash_equilibrium)
    fig.add_trace(go.Scatter(
        x=df.get("Period"), y=y,
        mode="lines",
        name="Nash-" + independent_variable_german,
        line=dict(color='black', dash="longdash")
    ))
    fig.update_layout(constants.layout, showlegend=False, title=treatment)
    fig.show(config=constants.config)
    time.sleep(3)

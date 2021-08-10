import time

import pandas as pd
import plotly.graph_objects as go
import constants

# Specify treatments to be analyzed
treatments = ["SIM-P-2", "SIM-P-3", "SIM-Q-2", "SIM-Q-3", "SEQ-P-2", "SEQ-P-3", "SEQ-Q-2", "SEQ-Q-3"]
periods_start = 9900
periods_end = 10000

for treatment in treatments:
    if treatment[4] == "P":
        independent_variable_english = "Price"
        independent_variable_german = "Preis"
    elif treatment[4] == "Q":
        independent_variable_english = "Quantity"
        independent_variable_german = "Menge"
    else:
        raise Exception("Invalid treatment!")

    df = pd.read_csv("data/trend/" + treatment + ".csv")

    fig = go.Figure(layout=go.Layout(
        xaxis=dict(title="Zeit"), yaxis=dict(title=independent_variable_german)
    ))
    fig.add_trace(go.Scatter(
        x=df.get("Period"), y=df.get(independent_variable_english + " of firm 1")[periods_start:periods_end],
        mode="lines",
        name=independent_variable_german + " von Firma 1"
    ))
    fig.add_trace(go.Scatter(
        x=df.get("Period"), y=df.get(independent_variable_english + " of firm 2")[periods_start:periods_end],
        mode="lines",
        name=independent_variable_german + " von Firma 2"
    ))
    if len(df.columns) > 7:
        fig.add_trace(go.Scatter(
            x=df.get("Period"), y=df.get(independent_variable_english + " of firm 3")[periods_start:periods_end],
            mode="lines",
            name=independent_variable_german + " von Firma 3"
        ))
    fig.update_xaxes(showticklabels=False)
    fig.update_layout(constants.layout, showlegend=False)
    fig.show(config=constants.config)
    time.sleep(3)
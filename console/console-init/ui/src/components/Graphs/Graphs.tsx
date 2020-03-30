import React from "react";
import {
  ChartArea,
  ChartContainer,
  ChartGroup,
  ChartLabel,
  ChartVoronoiContainer
} from "@patternfly/react-charts";

export const Graph: React.FunctionComponent<any> = () => {
  const basicGraph = (
    <div style={{ marginLeft: "0px", marginTop: "0px", height: "0px" }}>
      <div style={{ height: "50px", width: "200px" }}>
        <ChartGroup
          ariaDesc="Average number of pets"
          ariaTitle="Sparkline chart example"
          containerComponent={
            <ChartVoronoiContainer
              labels={({ datum }) => `${datum.name}: ${datum.y}`}
              constrainToVisibleArea
            />
          }
          height={100}
          maxDomain={{ y: 9 }}
          padding={0}
          width={400}
        >
          <ChartArea
            data={[
              { name: "Cats", x: "2015", y: 3 },
              { name: "Cats", x: "2016", y: 4 },
              { name: "Cats", x: "2017", y: 8 },
              { name: "Cats", x: "2018", y: 6 }
            ]}
          />
        </ChartGroup>
      </div>
      <ChartContainer>
        <ChartLabel text="CPU utilization" dy={15} />
      </ChartContainer>
    </div>
  );
  return basicGraph;
};

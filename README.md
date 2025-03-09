# CPU Scheduling Simulator
A Java-based GUI application to simulate and visualize various CPU scheduling algorithms.

# Overview
This CPU Scheduling Simulator provides a visual and intuitive way to understand different CPU scheduling algorithms commonly taught in operating systems courses. The application allows users to simulate the execution of multiple processes using different scheduling policies and visualizes the results through a Gantt chart and performance metrics.

# Features

Multiple Scheduling Algorithms:<br/>
First-Come, First-Served (FCFS)<br/>
Shortest Job Next (SJN) / Shortest Job First (SJF)<br/>
Round Robin (RR)<br/>
Priority Scheduling<br/>
Preemptive variants of SJN (SRTF) and Priority Scheduling<br/>

Interactive GUI:
Configurable number of processes
Adjustable time quantum for Round Robin
Option to toggle between preemptive and non-preemptive modes

Visual Representations:
Gantt chart visualization showing the execution sequence of processes
Tabular output showing key performance metrics for each process
Color-coded execution blocks for easy identification

Performance Metrics:
Process ID (PID)<br/>
Arrival Time (AT)<br/>
Burst Time (BT)<br/>
Waiting Time (WT)<br/>
Turnaround Time (TAT)<br/>
Average Waiting Time<br/>
Average Turnaround Time



# How to Use

Enter the number of processes to simulate
Set the time quantum (for Round Robin algorithm)
Select the scheduling algorithm from the dropdown menu
Choose preemptive mode if applicable (for SJN and Priority Scheduling)
Click "Run Simulation"
View the results in the Gantt chart and metrics table

# Algorithms Explained
## First-Come, First-Served (FCFS)
Processes are executed in the order they arrive in the ready queue. This is a non-preemptive algorithm where the CPU is allocated to the process until it terminates or performs I/O.

## Shortest Job Next (SJN)
Also known as Shortest Job First (SJF), this algorithm selects the process with the smallest burst time. In non-preemptive mode, once a process begins execution, it runs to completion.

## Shortest Remaining Time First (SRTF)
This is the preemptive version of SJN where the process with the shortest remaining time is selected for execution. If a new process arrives with a shorter burst time than the remaining time of the current process, the CPU is allocated to the new process.

## Round Robin (RR)
This algorithm allocates the CPU to each process for a fixed time interval (time quantum). After the time quantum expires, the process is preempted and added to the end of the ready queue.

## Priority Scheduling
Processes are scheduled according to their priority values. In non-preemptive mode, once a process begins execution, it continues until completion. In preemptive mode, if a higher priority process arrives, the current process is preempted.

# Implementation Details
The application is built using Java Swing for the GUI components. Key classes include:

.Process: Represents a process with its attributes (PID, arrival time, burst time, etc.)<br/>
.CPUScheduling: Main class containing the GUI setup and algorithm implementations<br/>
.GanttChartPanel: Custom JPanel for rendering the Gantt chart visualization<br/>
.GanttSlot: Represents a time slot in the Gantt chart<br/>
The simulation uses random values for process attributes but can be extended to support user input for each process.

# Technical Requirements
.Java Runtime Environment (JRE) 16 or higher<br/>
.Graphical environment to display the GUI

# Future Enhancements
.Add more scheduling algorithms (e.g., Multilevel Queue, Multilevel Feedback Queue)<br/>
.Allow manual entry of process details<br/>
.Add I/O burst support for more realistic simulations<br/>
.Implement step-by-step visualization to better understand algorithm execution<br/>
.Export results to CSV or PDF<br/>
.Add context switching overhead simulation<br/>

# Contributions
Contributions are welcome! Feel free to submit pull requests or open issues if you have suggestions for improvements or bug fixes.

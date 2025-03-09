import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

class Process {
    int pid, arrivalTime, burstTime, priority;
    int waitingTime, turnaroundTime, startTime, completionTime;
    int remainingTime;
    
    public Process(int pid, int arrivalTime, int burstTime, int priority) {
        this.pid = pid;
        this.arrivalTime = arrivalTime;
        this.burstTime = burstTime;
        this.priority = priority;
        this.remainingTime = burstTime;
    }
}

public class CPUScheduling {
    private static JTextArea resultArea;
    private static GanttChartPanel ganttChartPanel;
    private static JCheckBox preemptiveCheckBox;
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(CPUScheduling::createGUI);
    }
    
    private static void createGUI() {
        JFrame frame = new JFrame("CPU Scheduling Simulator");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLayout(new BorderLayout());
        
        JPanel inputPanel = new JPanel(new GridLayout(6, 2, 5, 5));
        
        JLabel labelProcesses = new JLabel("Number of Processes:");
        JTextField textProcesses = new JTextField();
        
        JLabel labelQuantum = new JLabel("Time Quantum (RR only):");
        JTextField textQuantum = new JTextField();
        
        JLabel labelAlgorithm = new JLabel("Select Algorithm:");
        String[] algorithms = {"FCFS", "SJN", "Round Robin", "Priority Scheduling"};
        JComboBox<String> comboAlgorithm = new JComboBox<>(algorithms);
        
        // Add preemptive option (only applicable for SJN and Priority)
        preemptiveCheckBox = new JCheckBox("Preemptive Mode");
        preemptiveCheckBox.setEnabled(false);  // Initially disabled
        
        JButton btnRun = new JButton("Run Simulation");
        
        resultArea = new JTextArea();
        resultArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(resultArea);
        
        ganttChartPanel = new GanttChartPanel();
        
        inputPanel.add(labelProcesses);
        inputPanel.add(textProcesses);
        inputPanel.add(labelQuantum);
        inputPanel.add(textQuantum);
        inputPanel.add(labelAlgorithm);
        inputPanel.add(comboAlgorithm);
        inputPanel.add(new JLabel(""));  // Spacer
        inputPanel.add(preemptiveCheckBox);
        inputPanel.add(new JLabel());
        inputPanel.add(btnRun);
        
        // Add listener to enable/disable preemptive checkbox based on algorithm selection
        comboAlgorithm.addActionListener(e -> {
            String selectedAlgorithm = (String) comboAlgorithm.getSelectedItem();
            // Enable preemptive checkbox only for SJN and Priority Scheduling
            preemptiveCheckBox.setEnabled("SJN".equals(selectedAlgorithm) || 
                                         "Priority Scheduling".equals(selectedAlgorithm));
            if (!preemptiveCheckBox.isEnabled()) {
                preemptiveCheckBox.setSelected(false);
            }
        });
        
        frame.add(inputPanel, BorderLayout.NORTH);
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(ganttChartPanel, BorderLayout.SOUTH);
        
        btnRun.addActionListener(e -> {
            try {
                int n = Integer.parseInt(textProcesses.getText());
                int quantum = textQuantum.getText().isEmpty() ? 1 : Integer.parseInt(textQuantum.getText());
                String selectedAlgorithm = (String) comboAlgorithm.getSelectedItem();
                boolean isPreemptive = preemptiveCheckBox.isSelected();
                
                List<Process> processes = generateProcesses(n);
                
                switch (selectedAlgorithm) {
                    case "FCFS" -> fcfs(processes);
                    case "SJN" -> {
                        if (isPreemptive) {
                            srtf(processes);  // Shortest Remaining Time First (preemptive SJN)
                        } else {
                            sjn(processes);   // Non-preemptive SJN
                        }
                    }
                    case "Round Robin" -> roundRobin(processes, quantum);
                    case "Priority Scheduling" -> {
                        if (isPreemptive) {
                            preemptivePriority(processes);  // Preemptive Priority
                        } else {
                            priorityScheduling(processes);   // Non-preemptive Priority
                        }
                    }
                }
            } catch (NumberFormatException ex) {
                resultArea.setText("Please enter valid numbers.");
            }
        });
        
        frame.setVisible(true);
    }
    
    static List<Process> generateProcesses(int n) {
        List<Process> processes = new ArrayList<>();
        Random rand = new Random();
        for (int i = 0; i < n; i++) {
            processes.add(new Process(i + 1, rand.nextInt(5), rand.nextInt(10) + 1, rand.nextInt(5) + 1));
        }
        return processes;
    }
    
    static void fcfs(List<Process> processes) {
        // Create a copy and sort by arrival time
        List<Process> sortedProcesses = new ArrayList<>(processes);
        sortedProcesses.sort(Comparator.comparingInt(p -> p.arrivalTime));
        
        int currentTime = 0;
        List<GanttSlot> ganttSlots = new ArrayList<>();
        
        for (Process p : sortedProcesses) {
            // Process starts either at current time or arrival time (whichever is later)
            p.startTime = Math.max(currentTime, p.arrivalTime);
            currentTime = p.startTime;
            
            // Process completes after its burst time
            p.completionTime = currentTime + p.burstTime;
            
            // Add to Gantt chart
            ganttSlots.add(new GanttSlot(p.pid, p.startTime, p.completionTime));
            
            currentTime = p.completionTime;
            
            // Calculate waiting and turnaround times
            p.turnaroundTime = p.completionTime - p.arrivalTime;
            p.waitingTime = p.turnaroundTime - p.burstTime;
        }
        
        // Update the original processes with the calculated values
        for (Process original : processes) {
            for (Process sorted : sortedProcesses) {
                if (original.pid == sorted.pid) {
                    original.startTime = sorted.startTime;
                    original.completionTime = sorted.completionTime;
                    original.waitingTime = sorted.waitingTime;
                    original.turnaroundTime = sorted.turnaroundTime;
                    break;
                }
            }
        }
        
        ganttChartPanel.setGanttSlots(ganttSlots);
        displayResults(processes);
    }
    
    static void sjn(List<Process> processes) {
        // Create a deep copy of processes
        List<Process> processCopy = new ArrayList<>();
        for (Process p : processes) {
            Process newP = new Process(p.pid, p.arrivalTime, p.burstTime, p.priority);
            processCopy.add(newP);
        }
        
        List<Process> readyQueue = new ArrayList<>();
        List<Process> completed = new ArrayList<>();
        List<GanttSlot> ganttSlots = new ArrayList<>();
        
        int currentTime = 0;
        
        while (completed.size() < processCopy.size()) {
            // Add newly arrived processes to ready queue
            for (Process p : processCopy) {
                if (p.arrivalTime <= currentTime && !readyQueue.contains(p) && !completed.contains(p)) {
                    readyQueue.add(p);
                }
            }
            
            if (readyQueue.isEmpty()) {
                currentTime++;
                continue;
            }
            
            // Find process with shortest burst time
            Process shortest = readyQueue.get(0);
            for (Process p : readyQueue) {
                if (p.burstTime < shortest.burstTime) {
                    shortest = p;
                }
            }
            
            // Execute the process
            shortest.startTime = currentTime;
            shortest.completionTime = currentTime + shortest.burstTime;
            shortest.turnaroundTime = shortest.completionTime - shortest.arrivalTime;
            shortest.waitingTime = shortest.turnaroundTime - shortest.burstTime;
            
            // Add to Gantt chart
            ganttSlots.add(new GanttSlot(shortest.pid, shortest.startTime, shortest.completionTime));
            
            currentTime = shortest.completionTime;
            readyQueue.remove(shortest);
            completed.add(shortest);
        }
        
        // Update original processes with calculated times
        for (Process original : processes) {
            for (Process comp : completed) {
                if (original.pid == comp.pid) {
                    original.startTime = comp.startTime;
                    original.completionTime = comp.completionTime;
                    original.waitingTime = comp.waitingTime;
                    original.turnaroundTime = comp.turnaroundTime;
                    break;
                }
            }
        }
        
        ganttChartPanel.setGanttSlots(ganttSlots);
        displayResults(processes);
    }
    
    static void srtf(List<Process> processes) {
        // Create a deep copy of processes
        List<Process> processCopy = new ArrayList<>();
        for (Process p : processes) {
            Process newP = new Process(p.pid, p.arrivalTime, p.burstTime, p.priority);
            processCopy.add(newP);
        }
        
        // Sort by arrival time
        processCopy.sort(Comparator.comparingInt(p -> p.arrivalTime));
        
        int n = processCopy.size();
        int complete = 0;
        int currentTime = 0;
        int prevProcess = -1;
        
        // To track process execution for Gantt chart
        List<GanttSlot> ganttSlots = new ArrayList<>();
        int lastPid = -1;
        int lastStart = 0;
        
        // Initialize remaining time for all processes
        for (Process p : processCopy) {
            p.remainingTime = p.burstTime;
        }
        
        // Run until all processes are completed
        while (complete != n) {
            // Find process with minimum remaining time at current time
            int minRemaining = Integer.MAX_VALUE;
            int shortestIndex = -1;
            
            for (int i = 0; i < n; i++) {
                Process p = processCopy.get(i);
                if (p.arrivalTime <= currentTime && p.remainingTime < minRemaining && p.remainingTime > 0) {
                    minRemaining = p.remainingTime;
                    shortestIndex = i;
                }
            }
            
            if (shortestIndex == -1) {
                // No process available, advance time
                currentTime++;
                continue;
            }
            
            Process currentProcess = processCopy.get(shortestIndex);
            
            // Check if there's a context switch (different process from previous time unit)
            if (prevProcess != currentProcess.pid) {
                // If we had a previous process, end its slot
                if (prevProcess != -1 && lastStart < currentTime) {
                    ganttSlots.add(new GanttSlot(prevProcess, lastStart, currentTime));
                }
                
                // Start new slot for current process
                lastStart = currentTime;
                prevProcess = currentProcess.pid;
                
                // Save start time for the process (earliest execution)
                if (currentProcess.startTime == 0 || currentTime < currentProcess.startTime) {
                    currentProcess.startTime = currentTime;
                }
            }
            
            // Reduce remaining time
            currentProcess.remainingTime--;
            currentTime++;
            
            // Check if process is completed
            if (currentProcess.remainingTime == 0) {
                complete++;
                prevProcess = -1; // Force context switch
                
                // Add final slot for this process
                ganttSlots.add(new GanttSlot(currentProcess.pid, lastStart, currentTime));
                
                currentProcess.completionTime = currentTime;
                currentProcess.turnaroundTime = currentProcess.completionTime - currentProcess.arrivalTime;
                currentProcess.waitingTime = currentProcess.turnaroundTime - currentProcess.burstTime;
            }
        }
        
        // Update original processes with calculated values
        for (Process original : processes) {
            for (Process copy : processCopy) {
                if (original.pid == copy.pid) {
                    original.startTime = copy.startTime;
                    original.completionTime = copy.completionTime;
                    original.waitingTime = copy.waitingTime;
                    original.turnaroundTime = copy.turnaroundTime;
                    break;
                }
            }
        }
        
        // Merge consecutive slots for same process (optional for cleaner chart)
        List<GanttSlot> mergedSlots = mergeGanttSlots(ganttSlots);
        
        ganttChartPanel.setGanttSlots(mergedSlots);
        displayResults(processes);
    }
    
    static void roundRobin(List<Process> processes, int quantum) {
        // Create deep copies to work with
        List<Process> tempProcesses = new ArrayList<>();
        for (Process p : processes) {
            Process newP = new Process(p.pid, p.arrivalTime, p.burstTime, p.priority);
            tempProcesses.add(newP);
        }
        
        tempProcesses.sort(Comparator.comparingInt(p -> p.arrivalTime));
        
        Queue<Process> readyQueue = new LinkedList<>();
        List<Process> completedProcesses = new ArrayList<>();
        
        int currentTime = 0;
        int processIndex = 0;
        
        // Track execution sequence for Gantt chart
        List<GanttSlot> ganttSlots = new ArrayList<>();
        
        while (completedProcesses.size() < processes.size()) {
            // Add newly arrived processes to ready queue
            while (processIndex < tempProcesses.size() && tempProcesses.get(processIndex).arrivalTime <= currentTime) {
                readyQueue.add(tempProcesses.get(processIndex));
                processIndex++;
            }
            
            if (readyQueue.isEmpty()) {
                currentTime++;
                continue;
            }
            
            Process currentProcess = readyQueue.poll();
            
            int executeTime = Math.min(quantum, currentProcess.remainingTime);
            int startExecution = currentTime;
            currentTime += executeTime;
            currentProcess.remainingTime -= executeTime;
            
            // Add to Gantt chart
            ganttSlots.add(new GanttSlot(currentProcess.pid, startExecution, currentTime));
            
            // Add newly arrived processes during execution
            while (processIndex < tempProcesses.size() && tempProcesses.get(processIndex).arrivalTime <= currentTime) {
                readyQueue.add(tempProcesses.get(processIndex));
                processIndex++;
            }
            
            if (currentProcess.remainingTime > 0) {
                readyQueue.add(currentProcess);
            } else {
                // Process is complete
                currentProcess.completionTime = currentTime;
                currentProcess.turnaroundTime = currentProcess.completionTime - currentProcess.arrivalTime;
                currentProcess.waitingTime = currentProcess.turnaroundTime - currentProcess.burstTime;
                completedProcesses.add(currentProcess);
            }
        }
        
        // Find start times from Gantt chart and update original processes
        Map<Integer, Integer> firstStartTimes = new HashMap<>();
        for (GanttSlot slot : ganttSlots) {
            if (!firstStartTimes.containsKey(slot.pid) || slot.startTime < firstStartTimes.get(slot.pid)) {
                firstStartTimes.put(slot.pid, slot.startTime);
            }
        }
        
        for (Process p : processes) {
            // Find the matching completed process
            for (Process comp : completedProcesses) {
                if (p.pid == comp.pid) {
                    p.completionTime = comp.completionTime;
                    p.waitingTime = comp.waitingTime;
                    p.turnaroundTime = comp.turnaroundTime;
                    p.startTime = firstStartTimes.getOrDefault(p.pid, 0);
                    break;
                }
            }
        }
        
        ganttChartPanel.setGanttSlots(ganttSlots);
        displayResults(processes);
    }
    
    static void priorityScheduling(List<Process> processes) {
        // Create a deep copy of processes
        List<Process> processCopy = new ArrayList<>();
        for (Process p : processes) {
            Process newP = new Process(p.pid, p.arrivalTime, p.burstTime, p.priority);
            processCopy.add(newP);
        }
        
        List<Process> readyQueue = new ArrayList<>();
        List<Process> completed = new ArrayList<>();
        List<GanttSlot> ganttSlots = new ArrayList<>();
        
        int currentTime = 0;
        
        while (completed.size() < processCopy.size()) {
            // Add newly arrived processes to ready queue
            for (Process p : processCopy) {
                if (p.arrivalTime <= currentTime && !readyQueue.contains(p) && !completed.contains(p)) {
                    readyQueue.add(p);
                }
            }
            
            if (readyQueue.isEmpty()) {
                currentTime++;
                continue;
            }
            
            // Find process with highest priority (lower number = higher priority)
            Process highestPriority = readyQueue.get(0);
            for (Process p : readyQueue) {
                if (p.priority < highestPriority.priority) {
                    highestPriority = p;
                }
            }
            
            // Execute the process
            highestPriority.startTime = currentTime;
            highestPriority.completionTime = currentTime + highestPriority.burstTime;
            highestPriority.turnaroundTime = highestPriority.completionTime - highestPriority.arrivalTime;
            highestPriority.waitingTime = highestPriority.turnaroundTime - highestPriority.burstTime;
            
            // Add to Gantt chart
            ganttSlots.add(new GanttSlot(highestPriority.pid, highestPriority.startTime, highestPriority.completionTime));
            
            currentTime = highestPriority.completionTime;
            readyQueue.remove(highestPriority);
            completed.add(highestPriority);
        }
        
        // Update original processes with calculated times
        for (Process original : processes) {
            for (Process comp : completed) {
                if (original.pid == comp.pid) {
                    original.startTime = comp.startTime;
                    original.completionTime = comp.completionTime;
                    original.waitingTime = comp.waitingTime;
                    original.turnaroundTime = comp.turnaroundTime;
                    break;
                }
            }
        }
        
        ganttChartPanel.setGanttSlots(ganttSlots);
        displayResults(processes);
    }
    
    static void preemptivePriority(List<Process> processes) {
        // Create a deep copy of processes
        List<Process> processCopy = new ArrayList<>();
        for (Process p : processes) {
            Process newP = new Process(p.pid, p.arrivalTime, p.burstTime, p.priority);
            processCopy.add(newP);
        }
        
        // Sort by arrival time
        processCopy.sort(Comparator.comparingInt(p -> p.arrivalTime));
        
        int n = processCopy.size();
        int complete = 0;
        int currentTime = 0;
        int prevProcess = -1;
        
        // To track process execution for Gantt chart
        List<GanttSlot> ganttSlots = new ArrayList<>();
        int lastStart = 0;
        
        // Initialize remaining time for all processes
        for (Process p : processCopy) {
            p.remainingTime = p.burstTime;
        }
        
        // Run until all processes are completed
        while (complete != n) {
            // Find process with highest priority at current time
            int highestPriority = Integer.MAX_VALUE;
            int selectedIndex = -1;
            
            for (int i = 0; i < n; i++) {
                Process p = processCopy.get(i);
                if (p.arrivalTime <= currentTime && p.priority < highestPriority && p.remainingTime > 0) {
                    highestPriority = p.priority;
                    selectedIndex = i;
                }
            }
            
            if (selectedIndex == -1) {
                // No process available, advance time
                currentTime++;
                continue;
            }
            
            Process currentProcess = processCopy.get(selectedIndex);
            
            // Check if there's a context switch (different process from previous time unit)
            if (prevProcess != currentProcess.pid) {
                // If we had a previous process, end its slot
                if (prevProcess != -1 && lastStart < currentTime) {
                    ganttSlots.add(new GanttSlot(prevProcess, lastStart, currentTime));
                }
                
                // Start new slot for current process
                lastStart = currentTime;
                prevProcess = currentProcess.pid;
                
                // Save start time for the process (earliest execution)
                if (currentProcess.startTime == 0 || currentTime < currentProcess.startTime) {
                    currentProcess.startTime = currentTime;
                }
            }
            
            // Reduce remaining time
            currentProcess.remainingTime--;
            currentTime++;
            
            // Check if process is completed
            if (currentProcess.remainingTime == 0) {
                complete++;
                prevProcess = -1; // Force context switch
                
                // Add final slot for this process
                ganttSlots.add(new GanttSlot(currentProcess.pid, lastStart, currentTime));
                
                currentProcess.completionTime = currentTime;
                currentProcess.turnaroundTime = currentProcess.completionTime - currentProcess.arrivalTime;
                currentProcess.waitingTime = currentProcess.turnaroundTime - currentProcess.burstTime;
            }
        }
        
        // Update original processes with calculated values
        for (Process original : processes) {
            for (Process copy : processCopy) {
                if (original.pid == copy.pid) {
                    original.startTime = copy.startTime;
                    original.completionTime = copy.completionTime;
                    original.waitingTime = copy.waitingTime;
                    original.turnaroundTime = copy.turnaroundTime;
                    break;
                }
            }
        }
        
        // Merge consecutive slots for same process (optional for cleaner chart)
        List<GanttSlot> mergedSlots = mergeGanttSlots(ganttSlots);
        
        ganttChartPanel.setGanttSlots(mergedSlots);
        displayResults(processes);
    }
    
    static List<GanttSlot> mergeGanttSlots(List<GanttSlot> slots) {
        if (slots == null || slots.isEmpty()) return slots;
        
        List<GanttSlot> merged = new ArrayList<>();
        GanttSlot current = slots.get(0);
        
        for (int i = 1; i < slots.size(); i++) {
            GanttSlot next = slots.get(i);
            if (current.pid == next.pid && current.endTime == next.startTime) {
                // Merge these slots
                current.endTime = next.endTime;
            } else {
                // Add the current slot and move to the next one
                merged.add(current);
                current = next;
            }
        }
        
        // Add the last slot
        merged.add(current);
        
        return merged;
    }
    
    static void displayResults(List<Process> processes) {
        StringBuilder sb = new StringBuilder("PID\tAT\tBT\tWT\tTAT\n");
        double avgWT = 0, avgTAT = 0;
        
        for (Process p : processes) {
            sb.append(p.pid).append("\t").append(p.arrivalTime).append("\t").append(p.burstTime)
              .append("\t").append(p.waitingTime).append("\t").append(p.turnaroundTime).append("\n");
            avgWT += p.waitingTime;
            avgTAT += p.turnaroundTime;
        }
        
        avgWT /= processes.size();
        avgTAT /= processes.size();
        
        sb.append("\nAverage Waiting Time: ").append(String.format("%.2f", avgWT));
        sb.append("\nAverage Turnaround Time: ").append(String.format("%.2f", avgTAT));
        
        resultArea.setText(sb.toString());
        ganttChartPanel.repaint(); // Ensure chart is repainted after setting data
    }
}

// Class to represent a time slot in the Gantt chart
class GanttSlot {
    int pid;
    int startTime;
    int endTime;
    
    public GanttSlot(int pid, int startTime, int endTime) {
        this.pid = pid;
        this.startTime = startTime;
        this.endTime = endTime;
    }
}

class GanttChartPanel extends JPanel {
    private List<Process> processes;
    private List<GanttSlot> ganttSlots;
    
    public GanttChartPanel() {
        setPreferredSize(new Dimension(800, 150));
        setBorder(BorderFactory.createTitledBorder("Gantt Chart"));
    }
    
    public void setProcesses(List<Process> processes) {
        this.processes = new ArrayList<>(processes);
        repaint();
    }
    
    public void setGanttSlots(List<GanttSlot> slots) {
        this.ganttSlots = slots;
        repaint();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        if ((processes == null || processes.isEmpty()) && (ganttSlots == null || ganttSlots.isEmpty())) {
            return;
        }
        
        int panelWidth = getWidth() - 40;
        int timelineY = 80;
        int blockHeight = 40;
        int blockY = timelineY - blockHeight - 5;
        
        // Find max completion time for scaling
        int maxTime = 0;
        if (ganttSlots != null && !ganttSlots.isEmpty()) {
            for (GanttSlot slot : ganttSlots) {
                maxTime = Math.max(maxTime, slot.endTime);
            }
        } else if (processes != null && !processes.isEmpty()) {
            for (Process p : processes) {
                maxTime = Math.max(maxTime, p.completionTime);
            }
        }
        
        if (maxTime == 0) return; // Nothing to draw
        
        // Calculate pixels per time unit
        double pixelsPerUnit = (double) panelWidth / maxTime;
        
        // Draw timeline
        g.setColor(Color.BLACK);
        g.drawLine(20, timelineY, 20 + (int)(maxTime * pixelsPerUnit), timelineY);
        
        // Draw Gantt chart blocks
        if (ganttSlots != null && !ganttSlots.isEmpty()) {
            // Use ganttSlots for visualization
            for (GanttSlot slot : ganttSlots) {
                int x = 20 + (int)(slot.startTime * pixelsPerUnit);
                int width = Math.max((int)((slot.endTime - slot.startTime) * pixelsPerUnit), 1);
                
                // Draw process block
                g.setColor(getColorForProcess(slot.pid));
                g.fillRect(x, blockY, width, blockHeight);
                
                // Draw border
                g.setColor(Color.BLACK);
                g.drawRect(x, blockY, width, blockHeight);
                
                // Draw process ID
                g.setColor(Color.WHITE);
                if (width > 20) {
                    g.drawString("P" + slot.pid, x + width/2 - 5, blockY + blockHeight/2 + 5);
                }
                
                // Draw start time
                g.setColor(Color.BLACK);
                g.drawString(String.valueOf(slot.startTime), x, timelineY + 15);
            }
            
            // Draw final completion time
            int lastX = 20 + (int)(ganttSlots.get(ganttSlots.size()-1).endTime * pixelsPerUnit);
            g.drawString(String.valueOf(ganttSlots.get(ganttSlots.size()-1).endTime), lastX, timelineY + 15);
        } else if (processes != null && !processes.isEmpty()) {
            // Fallback to processes if no ganttSlots are provided
            // Sort processes by start time
            List<Process> sortedProcesses = new ArrayList<>(processes);
            sortedProcesses.sort(Comparator.comparingInt(p -> p.startTime));
            
            for (Process p : sortedProcesses) {
                int x = 20 + (int)(p.startTime * pixelsPerUnit);
                int width = Math.max((int)((p.completionTime - p.startTime) * pixelsPerUnit), 1);
                
                // Draw process block
                g.setColor(getColorForProcess(p.pid));
                g.fillRect(x, blockY, width, blockHeight);
                
                // Draw border
                g.setColor(Color.BLACK);
                g.drawRect(x, blockY, width, blockHeight);
                
                // Draw process ID
                g.setColor(Color.WHITE);
                if (width > 20) {
                    g.drawString("P" + p.pid, x + width/2 - 5, blockY + blockHeight/2 + 5);
                }
                
                // Draw start time
                g.setColor(Color.BLACK);
                g.drawString(String.valueOf(p.startTime), x, timelineY + 15);
            }
            
            // Draw final completion time
            if (!sortedProcesses.isEmpty()) {
                Process lastProcess = sortedProcesses.get(sortedProcesses.size() - 1);
                int lastX = 20 + (int)(lastProcess.completionTime * pixelsPerUnit);
                g.drawString(String.valueOf(lastProcess.completionTime), lastX, timelineY + 15);
            }
        }
    }
    
    private Color getColorForProcess(int pid) {
        // Generate a distinct color based on process ID
        switch (pid % 6) {
            case 0: return new Color(70, 130, 180);  // Steel Blue
            case 1: return new Color(220, 20, 60);   // Crimson
            case 2: return new Color(46, 139, 87);   // Sea Green
            case 3: return new Color(255, 140, 0);   // Dark Orange
            case 4: return new Color(138, 43, 226);  // Blue Violet
            case 5: return new Color(65, 105, 225);  // Royal Blue
            default: return new Color(0, 0, 0);      // Black (fallback)
        }
    }
}
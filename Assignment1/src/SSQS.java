import java.io.*;

public class SSQS {
    static final int Q_LIMIT = 100; // Limit on queue length
    static final int BUSY = 1;
    static final int IDLE = 0;


    static int next_event_type, num_custs_delayed, num_delays_required, num_events,
            num_in_q, server_status,event_count,customer_cnt;
    static float area_num_in_q, area_server_status, mean_interarrival, mean_service,
            sim_time, time_last_event, total_of_delays;
    static float[] time_arrival = new float[Q_LIMIT + 1];
    static float[] time_next_event = new float[3];


    static BufferedReader infile;
    static BufferedWriter outfile;
    static BufferedWriter eventfile;

    public static void main(String[] args) {
        try {
            infile = new BufferedReader(new FileReader("input.txt"));
            outfile = new BufferedWriter(new FileWriter("1905078_output.txt"));
            eventfile = new BufferedWriter(new FileWriter("events.txt"));
            num_events = 2;
            String line = infile.readLine();

            // Split the line into components based on space
            String[] parts = line.split(" ");

            // Parse the values
            mean_interarrival = Float.parseFloat(parts[0]);
            mean_service = Float.parseFloat(parts[1]);
            num_delays_required = Integer.parseInt(parts[2]);
            System.out.println(mean_interarrival);
            outfile.write("Single-server queueing system\n");
            outfile.write("Mean interarrival time\t\t" + String.format("%.3f", mean_interarrival) + " minutes\n");
            outfile.write("Mean service time\t\t\t" + String.format("%.3f", mean_service) + " minutes\n");
            outfile.write("Number of customers\t\t\t" + num_delays_required + "\n\n");

            /* Initialize the simulation. */
            initialize();

            /* Run the simulation while more delays are still needed. */
            while (num_custs_delayed < num_delays_required) {
                /* Determine the next event*/
                timing();
                /* Update time-average statistical accumulators. */
                update_time_avg_stats();
                /* Invoke the appropriate event function */
                switch (next_event_type) {
                    case 1:
                        arrive();
                        break;
                    case 2:
                        depart();
                        break;
                }
            }
            report();
            infile.close();
            outfile.close();
            eventfile.close();

        } catch (FileNotFoundException e) {
            System.out.println("File not found");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Method to initialize variables and simulation parameters
    public static void initialize() {
        /* Initialize the simulation clock. */
        sim_time = 0.0f;

        /* Initialize the state variables. */
        server_status = IDLE;
        num_in_q = 0;
        time_last_event = 0.0f;

        /* Initialize the statistical counters. */
        num_custs_delayed = 0;
        total_of_delays = 0.0f;
        area_num_in_q = 0.0f;
        area_server_status = 0.0f;

        /* Initialize event list.
           Since no customers are present, the departure (service completion)
           event is eliminated from consideration. */
        time_next_event[1] = sim_time + expon(mean_interarrival);
        time_next_event[2] = (float) 1.0e+30;
        event_count = 0;
        customer_cnt = 0;
    }

    /* Exponential variate generation function */
    public static float expon(float mean) {
        return (float) (-mean * Math.log(LCG.lcgrand(1)));
    }

    /*Timing Function*/
    public static void timing() throws IOException {
        int i;
        float min_time_next_event = (float) 1.0e+29;
        next_event_type = 0;

        /* Determine the event type of the next event to occur. */
        for (i = 1; i <= num_events; ++i) {
            if (time_next_event[i] < min_time_next_event) {
                min_time_next_event = time_next_event[i];
                next_event_type = i;
            }
        }

        /* Check to see whether the event list is empty. */
        if (next_event_type == 0) {
            // The event list is empty, so stop the simulation.
            outfile.write("\nEvent list empty at time " + sim_time);
            outfile.flush();
            System.exit(1); // Terminate the program
        }

        /* The event list is not empty, so advance the simulation clock. */
        sim_time = min_time_next_event;
    }

    /* Update area accumulators for time-average statistics. */
    public static void update_time_avg_stats() {
        float time_since_last_event;

        /* Compute time since last event, and update last-event-time marker. */
        time_since_last_event = sim_time - time_last_event;
        time_last_event = sim_time;

        /* Update area under number-in-queue function. */
        area_num_in_q += num_in_q * time_since_last_event;

        /* Update area under server-busy indicator function. */
        area_server_status += server_status * time_since_last_event;
    }

    /* Arrival event function */
    public static void arrive() throws IOException {
        float delay;

        /* Schedule next arrival. */
        time_next_event[1] = sim_time + expon(mean_interarrival);
        ++event_count;
        ++customer_cnt;
        eventfile.write(event_count+". Next event: Customer "+customer_cnt+" Arrival\n");
        /* Check to see whether server is busy. */
        if (server_status == BUSY) {
            /* Server is busy, so increment the number of customers in queue. */
            ++num_in_q;

            /* Check to see whether an overflow condition exists. */
            if (num_in_q > Q_LIMIT) {
                /* The queue has overflowed, so stop the simulation. */
                outfile.write("\nOverflow of the array time_arrival at");
                outfile.write(" time " + sim_time);
                outfile.flush();
                System.exit(2);
            }

            /* There is still room in the queue, so store the time of arrival of the
               arriving customer at the (new) end of time_arrival. */
            time_arrival[num_in_q] = sim_time;
        } else {
            /* Server is idle, so arriving customer has a delay of zero. */
            delay = 0.0f;
            total_of_delays += delay;

            /* Increment the number of customers delayed, and make the server busy. */
            ++num_custs_delayed;
            server_status = BUSY;

            /* Schedule a departure (service completion). */
            time_next_event[2] = sim_time + expon(mean_service);

            eventfile.write("\n---------No. of customers delayed: "+num_custs_delayed+"--------\n\n");
        }
    }

    /* Departure event function */

    public static void depart() throws IOException {
        int i;
        float delay;
        ++event_count;
        eventfile.write(event_count+". Next event: Customer "+num_custs_delayed+" Departure\n");

        /* Check to see whether the queue is empty. */
        if (num_in_q == 0) {
            /* The queue is empty, so make the server idle and eliminate the
               departure (service completion) event from consideration. */
            server_status = IDLE;
           // Eliminate departure event from consideration
            time_next_event[2] = (float) 1.0e+30; // Represents 1.0e+30 (very large number)
        } else {
            /* The queue is non-empty, so decrement the number of customers in queue. */
            --num_in_q;

            /* Compute the delay of the customer who is beginning service and update
               the total delay accumulator. */
            delay = sim_time - time_arrival[1];
            total_of_delays += delay;

            /* Increment the number of customers delayed, and schedule departure. */
            ++num_custs_delayed;
            time_next_event[2] = sim_time + expon(mean_service);


            /* Move each customer in queue (if any) up one place. */
            for (i = 1; i <= num_in_q; ++i) {
                time_arrival[i] = time_arrival[i + 1];
            }

            eventfile.write("\n---------No. of customers delayed: "+num_custs_delayed+"--------\n\n");
        }
    }

    public static void report() throws IOException {
        /* Compute and write estimates of desired measures of performance. */
        if (num_custs_delayed > 0) {
            outfile.write("\nAverage delay in queue\t\t" + String.format("%.3f", total_of_delays / num_custs_delayed) + " minutes\n");
        } else {
            outfile.write("\nAverage delay in queue\t\t0.000 minutes\n");
        }

        outfile.write("Average number in queue\t\t" + String.format("%.3f", area_num_in_q / sim_time) + "\n");
        outfile.write("Server utilization\t\t\t" + String.format("%.3f", area_server_status / sim_time) + "\n");
        outfile.write("Time simulation ended\t\t" + String.format("%.3f", sim_time) + " minutes\n");
        outfile.flush();
    }




}

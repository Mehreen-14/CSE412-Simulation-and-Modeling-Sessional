import java.io.*;

public class SPIS {
    static int  amount, bigs, initial_inv_level, inv_level, next_event_type, num_events,
            num_months, num_values_demand, smalls;
    static float  area_holding, area_shortage, holding_cost, incremental_cost, maxlag,
    mean_interdemand, minlag, setup_cost, shortage_cost, sim_time, time_last_event, total_ordering_cost;
    static float[] prob_distrib_demand = new float[26];
    static float[] time_next_event = new float[5]; // Time for next event (0-indexed)

    static BufferedReader infile;
    static PrintWriter outfile;


    public static void main(String[] args) {
        int i, num_policies;
        try{
            /* Open input and output files. */
            infile = new BufferedReader(new FileReader("in.txt"));
            outfile = new PrintWriter(new BufferedWriter(new FileWriter("output_1905078.txt")));

            /* Specify the number of events for the timing function. */
            num_events = 4;
            /* Read input parameters. */
            String line = infile.readLine();
            String[] parts = line.split(" ");

            initial_inv_level = Integer.parseInt(parts[0]);
            num_months = Integer.parseInt(parts[1]);
            num_policies = Integer.parseInt(parts[2]);
            System.out.println(initial_inv_level +" "+num_months+" "+num_policies);

            line = infile.readLine();
            parts = line.split(" ");
            num_values_demand = Integer.parseInt(parts[0]);
            mean_interdemand = Float.parseFloat(parts[1]);
            System.out.println(num_values_demand+" "+mean_interdemand);

            line = infile.readLine();
            parts = line.split(" ");
            setup_cost = Float.parseFloat(parts[0]);
            incremental_cost = Float.parseFloat(parts[1]);
            holding_cost = Float.parseFloat(parts[2]);
            shortage_cost = Float.parseFloat(parts[3]);
            System.out.println(setup_cost+" "+incremental_cost+" "+holding_cost+" "+shortage_cost);

            line = infile.readLine();
            parts = line.split(" ");
            minlag = Float.parseFloat(parts[0]);
            maxlag = Float.parseFloat(parts[1]);
            System.out.println(minlag+" "+maxlag);

            line = infile.readLine();
            parts = line.split(" ");
            for (i=1;i<=num_values_demand;i++)
            {
                prob_distrib_demand[i] = Float.parseFloat(parts[i-1]);
                System.out.print(prob_distrib_demand[i]+" ");
            }

            System.out.println();
            /* Write report heading and input parameters. */
            outfile.write("------Single-Product Inventory System------\n\n");
            outfile.write("Initial inventory level: "+initial_inv_level+" items\n\n");
            outfile.write("Number of demand sizes: "+num_values_demand+"\n\n");
            outfile.write("Distribution function of demand sizes: ");
            for (i=1;i<=num_values_demand;i++)
            {
                outfile.printf("%.2f ", prob_distrib_demand[i]);
            }
            outfile.printf("\n\nMean inter-demand time: %.2f months\n\n",mean_interdemand);
            outfile.printf("Delivery lag range: %.2f to %.2f months\n\n", minlag, maxlag);
            outfile.write("Length of simulation: "+num_months+" months\n\n");
            outfile.write("Costs:\n");
            outfile.printf("K = %.2f\ni = %.2f\nh = %.2f\npi = %.2f\n\n",setup_cost,incremental_cost,holding_cost,shortage_cost);
            outfile.write("Number of policies: "+num_policies+"\n\n");
            outfile.write("Policies:\n");
            outfile.write("--------------------------------------------------------------------------------------------------\n");
            outfile.printf(" %-13s  %-18s  %-18s  %-18s  %-18s \n", "Policy", "Avg_total_cost", "Avg_ordering_cost", "Avg_holding_cost", "Avg_shortage_cost");
            outfile.write("--------------------------------------------------------------------------------------------------\n\n");
            /* Run the simulation varying the inventory policy. */
            for (i=1;i<=num_policies;i++){
                /* Read the inventory policy, and initialize the simulation. */
                line = infile.readLine();
                parts = line.split(" ");
                smalls = Integer.parseInt(parts[0]);
                bigs = Integer.parseInt(parts[1]);
                initialize();
                 /* Run the simulation until it terminates after an end-simulation event
           (type 3) occurs.
                  */
                do {
                    /* Determine the next event. */
                    timing();
                    /* Update time-average statistical accumulators. */
                    update_time_avg_stats();
                    /* Invoke the appropriate event function. */
                    switch (next_event_type) {
                        case 1:
                            order_arrival();
                            break;
                        case 2:
                            demand();
                            break;
                        case 4:
                            evaluate();
                            break;
                        case 3:
                            report();
                            break;
                    }
        /* If the event just executed was not the end-simulation event (type 3),
           continue simulating.  Otherwise, end the simulation for the current
           (s,S) pair and go on to the next pair (if any). */
                } while (next_event_type != 3);
            }

            outfile.write("--------------------------------------------------------------------------------------------------\n\n");

            /* End the simulations. */

            infile.close();
            outfile.close();


        }
        catch (FileNotFoundException e) {
            System.out.println("File not found");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static int random_integer(float prob_distrib[]){
        int i;
        float u;

        /* Generate a U(0,1) random variate. */
        u = LCG.lcgrand(1);
        for (i = 1; u >= prob_distrib[i]; ++i)
            ;
        return i;
    }

    public static float uniform(float a, float b){
        /* Return a U(a,b) random variate. */
        return a + LCG.lcgrand(1) * (b - a);
    }

    public static void initialize() {
        /* Initialize the simulation clock. */
        sim_time = 0.0f;

        /* Initialize the state variables. */
        inv_level       = initial_inv_level;
        time_last_event = 0.0f;
        /* Initialize the statistical counters. */
        total_ordering_cost = 0.0f;
        area_holding        = 0.0f;
        area_shortage       = 0.0f;
    /* Initialize the event list.  Since no order is outstanding, the order
       arrival event is eliminated from consideration. */
        time_next_event[1] = (float) 1.0e+30;
        time_next_event[2] = sim_time + expon(mean_interdemand);
        time_next_event[3] = num_months;
        time_next_event[4] = 0.0f;
    }

    /* Exponential variate generation function */
    public static float expon(float mean) {
        return (float) (-mean * Math.log(LCG.lcgrand(1)));
    }
    public static void order_arrival(){
        /* Increment the inventory level by the amount ordered. */
        inv_level += amount;
    /* Since no order is now outstanding, eliminate the order-arrival event from
       consideration. */
        time_next_event[1] = (float) 1.0e+30;
    }


    public static void demand(){
        /* Decrement the inventory level by a generated demand size. */
        inv_level -= random_integer(prob_distrib_demand);
        /* Schedule the time of the next demand. */
        time_next_event[2] = sim_time + expon(mean_interdemand);
    }

    public static void evaluate(){
        /* Check whether the inventory level is less than smalls. */
        if (inv_level < smalls) {
        /* The inventory level is less than smalls, so place an order for the
           appropriate amount. */
            amount               = bigs - inv_level;
            total_ordering_cost += setup_cost + incremental_cost * amount;
            /* Schedule the arrival of the order. */
            time_next_event[1] = sim_time + uniform(minlag, maxlag);
        }
    /* Regardless of the place-order decision, schedule the next inventory
       evaluation. */
        time_next_event[4] = (float) (sim_time + 1.0);
    }

    public static void report (){
        /* Compute and write estimates of desired measures of performance. */
        float avg_holding_cost, avg_ordering_cost, avg_shortage_cost;
        avg_ordering_cost = total_ordering_cost / num_months;
        avg_holding_cost  = holding_cost * area_holding / num_months;
        avg_shortage_cost = shortage_cost * area_shortage / num_months;

        outfile.printf("(%d,%3d)  %17.2f %17.2f %19.2f %20.2f\n\n", smalls,bigs,(avg_ordering_cost+avg_holding_cost+avg_shortage_cost),avg_ordering_cost,avg_holding_cost,avg_shortage_cost);
    }

    public static void timing(){
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

    public static void  update_time_avg_stats()  /* Update area accumulators for time-average  statistics. */ {

        float time_since_last_event;
        /* Compute time since last event, and update last-event-time marker. */
        time_since_last_event = sim_time - time_last_event;
        time_last_event = sim_time;
    /* Determine the status of the inventory level during the previous interval.
       If the inventory level during the previous interval was negative, update
       area_shortage.  If it was positive, update area_holding.  If it was zero,
       no update is needed. */
        if (inv_level < 0)
            area_shortage -= inv_level * time_since_last_event;
        else if (inv_level > 0)
            area_holding += inv_level * time_since_last_event;

    }

}

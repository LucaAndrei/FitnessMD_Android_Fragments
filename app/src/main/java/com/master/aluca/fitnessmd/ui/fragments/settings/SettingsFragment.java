package com.master.aluca.fitnessmd.ui.fragments.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.support.v4.app.Fragment;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.TextView;

import com.master.aluca.fitnessmd.R;
import com.master.aluca.fitnessmd.common.Constants;
import com.master.aluca.fitnessmd.common.util.SharedPreferencesManager;
import com.master.aluca.fitnessmd.common.webserver.WebserverManager;
import com.master.aluca.fitnessmd.service.FitnessMDService;
import com.master.aluca.fitnessmd.ui.PairDeviceActivity;

/**
 * Created by aluca on 11/3/16.
 */
public class SettingsFragment extends Fragment {

    private static final String LOG_TAG = "Fitness_SettingsLsnr";

    private static SettingsFragment mInstance = null;


    private Activity mActivity;
    private SharedPreferencesManager sharedPreferencesManager;
    private WebserverManager webserverManager;
    Button btnGender;
    Button btnYoB;
    Button btnWeight;
    Button btnHeight;
    Button btnPairDevice;
    Button btnSyncNow;
    Button btnEraseData;
    Button btnLogout;

    private FitnessMDService mService;

    private Dialog mDialog;


    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.d(LOG_TAG, "on service connected");
            FitnessMDService.FitnessMD_Binder binder = (FitnessMDService.FitnessMD_Binder) iBinder;
            mService = binder.getService();

            if (mService == null) {
                Log.e(LOG_TAG, "unable to connect to service");
                return;
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d(LOG_TAG, "on service disconnected");
            mService = null;
        }
    };


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mActivity = activity;
    }

    @Override
    public void onStart() {
        Log.d(LOG_TAG, "onStart()");
        super.onStart();

        Intent intent = new Intent(this.mActivity, FitnessMDService.class);
        if (!FitnessMDService.isServiceRunning()) {
            mActivity.startService(intent);
        }
        if (!mActivity.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE)) {
            Log.e(LOG_TAG, "Unable to bind to optical service");
        }
    }

    public void onStop() {
        Log.d(LOG_TAG, "onStop()");
        super.onStop();

        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
        if (mService != null) {
            mActivity.unbindService(mServiceConnection);
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(R.layout.layout_tab_settings, container, false);
    }


    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        View view = getView();
        if (view == null) {
            return;
        }

        Log.d(LOG_TAG, "SettingsFragment");
        sharedPreferencesManager = SharedPreferencesManager.getInstance(getActivity());
        webserverManager = WebserverManager.getInstance(getActivity());

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.LOGOUT_INTENT);
        getActivity().registerReceiver(mBroadcastReceiver, intentFilter);

        setup(view);
    }

    BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(LOG_TAG, "onReceive : " + intent.getAction());
            if (intent.getAction().equalsIgnoreCase(Constants.LOGOUT_INTENT)) {
                getActivity().finish();
            }
        }
    };

    public void setup(View view) {
        btnGender = (Button) view.findViewById(R.id.btnGender);
        btnYoB = (Button) view.findViewById(R.id.btnYoB);
        btnWeight = (Button) view.findViewById(R.id.btnWeight);
        btnHeight = (Button) view.findViewById(R.id.btnHeight);
        btnPairDevice = (Button) view.findViewById(R.id.btnPairDevice);
        btnSyncNow = (Button) view.findViewById(R.id.btnSyncNow);
        btnEraseData = (Button) view.findViewById(R.id.btnEraseData);
        btnLogout = (Button) view.findViewById(R.id.btnLogout);
        initUserData();
        setListeners(view);
    }

    protected void setListeners(View mainActivity) {
        Log.d(LOG_TAG, "setListeners");


        btnGender.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(LOG_TAG, "btnGender onClick");
                final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), AlertDialog.THEME_HOLO_LIGHT);
                builder.setTitle("Gender");
                builder.setSingleChoiceItems(Constants.GENDERS, -1, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        sharedPreferencesManager.setGender(Constants.GENDERS[which].toString());
                        btnGender.setText(Html.fromHtml("Gender<br /><small><small>" + Constants.GENDERS[which] + "</small></small>"));
                        Intent genderChangedIntent = new Intent(Constants.GENDER_CHANGED_INTENT);
                        genderChangedIntent.putExtra(Constants.GENDER_CHANGED_INTENT_BUNDLE_KEY, Constants.GENDERS[which]);
                        getActivity().sendBroadcast(genderChangedIntent);
                        dialog.dismiss();
                    }
                });
                AlertDialog levelDialog = builder.create();
                levelDialog.show();
            }
        });

        btnHeight.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(LOG_TAG, "btnHeight onClick");
                final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), AlertDialog.THEME_HOLO_LIGHT);
                LayoutInflater inflater = getActivity().getLayoutInflater();
                View theView = inflater.inflate(R.layout.layout_single_number_picker, null, false);
                final NumberPicker height_picker = (NumberPicker) theView.findViewById(R.id.single_number_picker);
                final TextView unitsOfMeasurement = (TextView) theView.findViewById(R.id.unitsOfMeasurement);

                Constants.setNumberPickerTextColor(height_picker, getActivity().getResources().getColor(R.color.tab_menu_background));

                int height = sharedPreferencesManager.getHeight();
                height_picker.setMinValue(Constants.HEIGHT_MIN_VALUE);
                height_picker.setMaxValue(Constants.HEIGHT_MAX_VALUE);
                height_picker.setValue(height);
                unitsOfMeasurement.setText("cm");

                builder.setView(theView)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Log.d(LOG_TAG, "height OK click");
                                int height = height_picker.getValue();
                                sharedPreferencesManager.setHeight(height);
                                btnHeight.setText(Html.fromHtml("Height<br /><small>" + height + " " + unitsOfMeasurement.getText().toString() + "</small>"));
                                Intent heightChangedIntent = new Intent(Constants.HEIGHT_CHANGED_INTENT);
                                heightChangedIntent .putExtra(Constants.HEIGHT_CHANGED_INTENT_BUNDLE_KEY, height);
                                getActivity().sendBroadcast(heightChangedIntent);
                            }
                        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(LOG_TAG, "height Cancel click");
                    }
                });
                AlertDialog levelDialog = builder.create();
                levelDialog.show();
            }
        });

        btnWeight.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(LOG_TAG, "btnWeight onClick");
                final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), AlertDialog.THEME_HOLO_LIGHT);
                LayoutInflater inflater = getActivity().getLayoutInflater();
                View theView = inflater.inflate(R.layout.layout_weight_picker, null, false);
                final NumberPicker kg_picker = (NumberPicker) theView.findViewById(R.id.kgPicker);
                final NumberPicker g_picker = (NumberPicker) theView.findViewById(R.id.gPicker);

                Constants.setNumberPickerTextColor(kg_picker, getActivity().getResources().getColor(R.color.tab_menu_background));
                Constants.setNumberPickerTextColor(g_picker, getActivity().getResources().getColor(R.color.tab_menu_background));

                float weight = sharedPreferencesManager.getWeight();
                int weight_decimal_part = Math.round(weight%1 * 10);

                kg_picker.setMinValue(Constants.WEIGHT_KG_MIN_VALUE);
                kg_picker.setMaxValue(Constants.WEIGHT_KG_MAX_VALUE);
                g_picker.setMinValue(Constants.WEIGHT_G_MIN_VALUE);
                g_picker.setMaxValue(Constants.WEIGHT_G_MAX_VALUE);

                kg_picker.setValue((int)weight/1);
                g_picker.setValue(weight_decimal_part);


                builder.setView(theView)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Log.d(LOG_TAG, "weight OK click");
                                float weight = kg_picker.getValue() + g_picker.getValue()/10f;
                                sharedPreferencesManager.setWeight(weight);
                                btnWeight.setText(Html.fromHtml("Weight<br /><small>" + weight + " kg</small>"));

                                Intent weightChangedIntent = new Intent(Constants.WEIGHT_CHANGED_INTENT);
                                weightChangedIntent.putExtra(Constants.WEIGHT_CHANGED_INTENT_BUNDLE_KEY, weight);
                                getActivity().sendBroadcast(weightChangedIntent );

                            }
                        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(LOG_TAG, "weight Cancel click");
                    }
                });
                AlertDialog levelDialog = builder.create();
                levelDialog.show();
            }
        });

        btnYoB.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(LOG_TAG, "btnYoB onClick");
                final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), AlertDialog.THEME_HOLO_LIGHT);
                LayoutInflater inflater = getActivity().getLayoutInflater();
                View theView = inflater.inflate(R.layout.layout_single_number_picker, null, false);
                final NumberPicker yob_picker = (NumberPicker) theView.findViewById(R.id.single_number_picker);

                Constants.setNumberPickerTextColor(yob_picker, getActivity().getResources().getColor(R.color.tab_menu_background));

                final TextView unitsOfMeasurement = (TextView) theView.findViewById(R.id.unitsOfMeasurement);

                int yob = sharedPreferencesManager.getYearOfBirth();
                yob_picker.setMinValue(Constants.YOB_MIN_VALUE);
                yob_picker.setMaxValue(Constants.YOB_MAX_VALUE);
                yob_picker.setValue(yob);
                unitsOfMeasurement.setText("");

                builder.setView(theView)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Log.d(LOG_TAG, "yob OK click");
                                int yob = yob_picker.getValue();
                                sharedPreferencesManager.setYearOfBirth(yob);
                                btnYoB.setText(Html.fromHtml("Year of birth<br /><small>" + yob + " " + unitsOfMeasurement.getText().toString() + "</small>"));

                                Intent yobChangedIntent = new Intent(Constants.YOB_CHANGED_INTENT);
                                yobChangedIntent   .putExtra(Constants.YOB_CHANGED_INTENT_BUNDLE_KEY, yob);
                                getActivity().sendBroadcast(yobChangedIntent );
                            }
                        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(LOG_TAG, "yob Cancel click");
                    }
                });
                AlertDialog levelDialog = builder.create();
                levelDialog.show();
            }
        });


        btnPairDevice.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(LOG_TAG, "btnPairDevice onClick");
                Intent intent = new Intent(getActivity(), PairDeviceActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getActivity().startActivity(intent);
            }
        });

        btnLogout.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(LOG_TAG, "btnLogout onClick");
                webserverManager.requestLogout();
                //sharedPreferencesManager.setLoggedIn(false);
                //getActivity().finish();
            }
        });

        btnSyncNow.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(LOG_TAG, "btnSyncNow onClick");
                ProgressDialog mProgressDialog = new ProgressDialog(getActivity());
                mProgressDialog.setMessage("Receiving data from server");
                mProgressDialog.show();
                if (mService.getWeightFromServer()) {
                    Constants.displayToastMessage(getActivity(), "Success getting weight from server");
                    mProgressDialog.dismiss();
                } else {
                    mProgressDialog.dismiss();
                    Constants.displayToastMessage(getActivity(), "Error getting weight from server");
                }
            }
        });

        btnEraseData.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(LOG_TAG, "btnEraseData onClick");
                final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), AlertDialog.THEME_HOLO_LIGHT);
                builder.setMessage(Html.fromHtml("This action cannot be undone. Are you sure you want to erase all data?"));
                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(LOG_TAG, "btnEraseData OK click");
                        mService.eraseAllData();
                    }
                }).setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(LOG_TAG, "btnEraseData Cancel click");
                    }
                });
                AlertDialog levelDialog = builder.create();
                levelDialog.show();


            }
        });
    }

    private void initUserData() {
        Log.d(LOG_TAG, "initUserData");

        String gender = sharedPreferencesManager.getGender();
        btnGender.setText(Html.fromHtml("Gender<br /><small><small>" + gender + "</small></small>"));

        int height = sharedPreferencesManager.getHeight();
        Log.d(LOG_TAG, "height : " + height);
        btnHeight.setText(Html.fromHtml("Height<br /><small>" + height + " cm</small>"));

        float weight = sharedPreferencesManager.getWeight();
        Log.d(LOG_TAG, "weight : " + weight);
        btnWeight.setText(Html.fromHtml("Weight<br /><small>" + weight + " kg</small>"));

        int yob = sharedPreferencesManager.getYearOfBirth();
        Log.d(LOG_TAG, "yob : " + yob);
        btnYoB.setText(Html.fromHtml("Year of birth<br /><small>" + yob + "</small>"));
    }

    @Override
    public void onDestroy() {
        Log.d(LOG_TAG, "onDestroy()");
        getActivity().unregisterReceiver(mBroadcastReceiver);
        super.onDestroy();
    }




}
